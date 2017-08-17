# 简单的分布式短链接服务

说到短链接，很多人应该在很多地方都见过或者使用过，比如微博，论坛等，这些社交平台往往对发文字数有限制。如果不使用短链接服务，在有限的字数限制下，引用过长链接会必然会占据大量篇幅，同时也会大大影响排版效果。关于短链接的其他方面话题这里不继续展开，有兴趣的同学可以参考我发的一篇博客 [短链接原理分析](https://segmentfault.com/a/1190000010660103)。

接下来说一下项目实现。本项目基于知乎[短 URL 系统是怎么设计的？](https://www.zhihu.com/question/29270034/answer/46446911)问题中知友 **iammutex** 的回答，实现了一个简单的分布式短链接服务。在[短 URL 系统是怎么设计的？](https://www.zhihu.com/question/29270034/answer/46446911)问题中，**iammutex** 对短链接服务的设计原理进行了详细的阐述，读完后深受启发。大家如果想了解短链接原理，推荐去看一下。本人临时在网上搭建了自己写的这个项目，有兴趣的同学可以去玩玩，地址为 [titizz.com](http://titizz.com)。废话不多说了，上图展示一下：
![首页](https://github.com/code4wt/short-url/raw/master/screenshots/index.png)

图1 首页

![链接压缩结果页](https://github.com/code4wt/short-url/raw/master/screenshots/index1.png)

图2 结果页

![404](https://github.com/code4wt/short-url/raw/master/screenshots/404.png)

图3 404页面

这里需要申明的是，404 页面直接是从 [https://nclud.com/404][4] 拷贝过来后修改的，不是自己写的。另外，由于本人前端水平近乎等于零，所以请忽视前端页面。

### 0x01 总体设计
本项目使用 nginx 作为反向代理，使用 Redis 缓存热点数据，使用 MySQL 持久化数据。总体结构图如下：

![分布式短链接服务总体设计](https://github.com/code4wt/short-url/raw/master/screenshots/arc.png)

图4 分布式短链接服务总体设计

nginx 配置简单的反向代理并不复杂，这里不展开讨论。在这个项目中缓存和数据的使用则可以细说一下，先从数据库说起。

### 0x02 使用 MySQL 数据库发号
本项目采用了关系型数据库 MySQL 作为后台数据库，结合数据表自增字段实现发号功能。项目中使用了6位62进制数进行编码，可获得约500亿个号码空间。为了提高拓展性和并发性，实现过程中对这500亿个号码空间进行划分，由不同的机器发不同号码段内的号码。最终将500亿个号码空间划分成1000份，产生1000个逻辑发号器，分别发尾号为0 ~ 999的号码。每发一个号码，发号器不是加1，而是加1000。举例说明一下，以1号发号器为例，1号发号器应该发1号、1001号、2001号等尾号为1的号码，而不是发1号、2号、3号等号码。

说完发号器，这里引入一个“初始号码(initial code)”概念与发号器相匹配，初始号码 0 ~ 999 对应发号器 0 ~ 999。初始号码单独存放于数据库的的 initial_code 表中，initial_code 使用自增字段分配初始号码，用于表明哪些初始号码已被其他机器上的服务占用。设计上，一个服务对应一个初始号码。每个服务在启动阶段会从 initial_code 表中获取一个独一无二的初始号码。简单的介绍初始号码后，接下来说明一下数据库的设计。在这个项目中，数据库中有两中不同类型的数据表，分别是 initial_code，用于存放初始号码记录。url_mapping_x 表，用于存放<号码，链接>数据，这里的 x 对应初始号码，即0 ~ 999。也就是说，数据库中会有 url_mapping_0，url_mapping_1 ... url_mapping_999，共1000张 url_mapping 表。表结构如下：

**initial_code 表结构**
<table>
  <tr>
    <th>字段</th>
    <th>类型</th>
    <th>属性</th>
  </tr>
  <tr>
    <td>code</td>
    <td>INT</td>
    <td>自增</td>
  </tr>
</table>

**url_mapping_x 表结构**
<table>
  <tr>
    <th>字段</th>
    <th>类型</th>
    <th>属性</th>
    <th>备注</th>
  </tr>
  <tr>
    <td>code</td>
    <td>BIGINT</td>
    <td>自增</td>
    <td>自增初始值等于x（0 <= x <= 999）</td>
  </tr>
  <tr>
    <td>url</td>
    <td>VARCHAR</td>
    <td></td>
    <td></td>
  </tr>
</table>

说完表结构，继续说说初始号码。如上面所说，服务每次启动时，会从 initial_code 表中获取一个独一无二的初始号码。但是并非每次获取的初始号码都是有效的，毕竟初始号码也就1000个，只要服务累积启动1000次，就会耗完所有的初始号码。所以当服务从 initial_code 表中获取到的初始号码大于999时，表明此初始号码无效。同时也表明 initial_code 表中的初始号码资源被用完了。是不是 initial_code 表中无有效的初始号码分配时，服务就不能再用了呢？实际上也不是，要考虑这样一种情况，在实际运行中，服务崩溃退出，但是该初始号码的号码空间还未用完。initial_code 表只记录了哪些初始号码被用过，但是没有记录初始号码号码空间剩余量，即号码空间是否可用。所以当无法从 initial_code 表获取有效的初始号码时，可以考虑“捡漏”。通过比较没有被使用的初始号码对应的 url_mapping 表中 code 字段最大值与整个号码空间的最大值的大小，来确定这个初始号码是否还有可用的号码空间。但通过比较的方式获取初始号码又会引入新的问题，即多个服务可能会获得同一个初始号码。这时就需要对获取初始号码的逻辑进行分布式加锁处理，即同一时刻，只运行一个服务调用获取初始号码逻辑。

上面说了一下短链接服务的初始号的分配策略实现细节，接下来再说说发号策略的具体实现。如本节一开始所说，发号策略每次发号都是加1000，而不是加1。在具体实现中，发号策略依赖数据表自增字段来是实现的。然而数据表自增字段每次是加1，而不是加1000。如果要加1000，则需设置全局变量 auto_increment_offset = 1000。但是设置后又会影响 initial_code 表中的自增字段，所以最终在数据库层面上仍然使用加1。但是返回给客户端时，则需做一些转换。这里以 url_mapping_2 为例，假设该表中现在存储了如下数据：
<table>
  <tr>
    <th>code</th>
    <th>url</th>
  </tr>
  <tr>
    <td>2</td>
    <td>https://segmentfault.com/u/code4fun</td>
  </tr>
  <tr>
    <td>3</td>
    <td>https://segmentfault.com/a/1190000010660103</td>
  </tr>
</table>

当 service 层向 url_mapping_2 表中插入 https://segmentfault.com/a/1190000010660103 数据时，返回给 service 层的号码不应该是3，而应该是1002。此时需要做一个简单的转换，转换公式如下：
```
real_code = (code - initial_code) * 1000 + initial_code
```
号码3和初始号码2带入其中计算就能得到 1002。这里说的是 dao 层返回给 service 层时做的转换，service 层向 dao 层查询数据时，也需将 real_code 转换为数据表中的 code。以 real_code = 1002 为例：
```
initial_code = real_code % 1000;
code = initial_code + real_code / 1000
     = real_code % 1000 + real_code / 1000;
```

上面的公式比较简单，大家有兴趣可以算算，口算就能算出结果。
至此，本项目中数据库设计部分就差不多讲完了。因为项目相对比较简单，加之目前没有对数据库层面进行深入测试。比如放入大量数据到 url_mapping 表中测试性能，以及大数据量下的 SQL 优化等。待后续有时间，我再将这部分的测试结果写出来，暂时这一节内容就写到这了。

### 0x03 使用 Redis 缓存数据
相对于数据库层面较为复杂的实现，缓存层面的实现则要简单的多。本项目使用 Redis 中间件作为缓存。主要缓存了如下数据：
<table>
  <tr>
    <th>key</th>
    <th>value</th>
    <th>过期时间</th>
    <th>说明</th>
  </tr>
  <tr>
    <td>code</td>
    <td>url</td>
    <td>1h</td>
    <td></td>
  </tr>
<tr>
    <td>url</td>
    <td>code</td>
    <td>1h</td>
    <td></td>
  <tr>
    <td>server-uuid</td>
    <td>Java UUID String</td>
    <td>5min</td>
    <td>服务的uuid，具体作用下面会展开说明</td>
  </tr>
  <tr>
    <td>in-use-initial-codes</td>
    <td>zset = [0, 1, 2, ... 999]</td>
    <td></td>
    <td>缓存了正在被使用的初始号码，这些初始号码存放于 zset 中</td>
  </tr>
  </tr>
</table>

在本项目中，缓存存储了<code, url> 和 <url, code> 键值对，过期时间为1小时。只要在1小时内访问该数据，过期时间会被重置。所以对于热点数据，该缓存策略可以很好的处理，可在一定程度上减轻数据库的压力。<code, url> 和 <url, code> 键值对缓存用途比较简单，没什么好说的。接下来说说 server-uuid 和 in-use-initial-codes 缓存。

先说说 in-use-initial-codes 缓存，in-use-initial-codes 缓存了所有的短链接服务正在使用的初始号码。在上一节说到过初始号码分配的策略，当 initial_code 表无法分配有效的初始号码时，就需要通过比较**没有被使用的**初始号码对应的 url_mapping 表中 code 字段最大值与整个号码空间的最大值的大小，来确定初始号码是否可用。注意加粗字体“没有被使用的”，怎样去确定哪些初始号码没有被使用？答案就是通过 in-use-initial-codes 缓存，除 in-use-initial-codes 缓存的初始号码，其他的初始号码都是未被使用的。in-use-initial-codes 缓存不仅仅只是缓存了当前正在使用的初始号码，同时还要引入过期机制，防止某个服务挂了后，相应的初始号码仍然还存储在缓存中，没有被释放。但由于 zset 不支持其内的数据过期的机制，所以需要我们自己实现一个过期机制。本项目使用 zset score 实现了一个过期机制，服务启动时会将初始号码写入 in-use-initial-codes 中，并将该初始号码的 score 设为当前时间。服务启动后，会定时更新其使用的初始号码的 score 为当前时间，这样就不会被清理程序清理掉。

说完 in-use-initial-codes 缓存，再来说说server-uuid 缓存。正如键名所示，该缓存存储了某个短链接服务的 UUID。不同的服务会定时去抢占这个缓存，并将缓存值设置为自己的 UUID。该缓存的用途只有一个，即表明哪个服务有权限清除 in-use-initial-codes 缓存中过期的初始号码。那么怎样清除过期号码？答案如下：
```
min = 0;
max = current_time - expired_time;
ZREMRANGEBYSCORE in-use-initial-codes min max 
```

### 0x04 总结
分布式短链接服务是我写的第一个分布式项目，尽管最终只是一个简单的实现，但是在这个过程中还是学到了一些经验。比如使用 Nginx 配置反向代理、多客户端使用 Redis 要关闭保护模式、多客户端连接 MySQL 时，MySQL 要先对客户端 ip 进行授权才可以，最后还有分布式锁的使用。尽管这些都是很普通的经验，但是我相信在后续不断的学习中，会积累更多的经验。这个项目是一个很好的开端，在接下来的时间里，还要不断的练习，所以 keep going！

  [1]: /img/bVSVEr
  [2]: /img/bVSVEB
  [3]: /img/bVSVEW
  [4]: https://nclud.com/404
  [5]: /img/bVSTC6