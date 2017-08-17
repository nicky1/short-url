//注意点　这个ＪＳ千万不要自作聪明添加一个入口函数
//这个js尽量放到最前面去，在页面的元素加载前，就把这个屏幕对应的fontSize计算完成
var oHtml = document.documentElement;
// 获取屏幕的宽度
var screenWidth = oHtml.offsetWidth;
// 设计图的宽度，根据自己的设计图去填写
var uiWidth = 640;
// 自己设定的html的font值
var fonts = 40;
var bili = uiWidth/fonts;
// 最开始的时候调用一次
getSize();
// resize的时候动态监听
window.addEventListener('resize', getSize);

function getSize(){
	screenWidth = oHtml.offsetWidth;
	// 如果说屏幕小于320 就限制在320对应的fontsize
	// 如果说大于设计图的宽度，就限制在设计图的宽度
	// 都不满足，就代表在正常的区间里面，就可以自由的动态计算
	if(screenWidth <= 320){
		oHtml.style.fontSize = 320/bili + 'px';
	}else if(screenWidth >= uiWidth){
		oHtml.style.fontSize = uiWidth/bili + 'px';
	}else{
		// 动态设置当前屏幕对应的html的font值
		oHtml.style.fontSize = screenWidth/bili + 'px';
	}	
}
