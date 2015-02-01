<img src="http://avatar.csdn.net/blogpic/20150127140257890.jpg">    
# SimpleImageLoader Framework
   A Simple ImageLoader for Android, articles for this framework <a href="http://blog.csdn.net/column/details/android-imageloader.html" target="_blank">How to make a ImageLoader Framework</a>.


## Useage 
1. init The SimpleImageLoader with ImageLoaderConfig
```java
 private void initImageLoader() {
        ImageLoaderConfig config = new ImageLoaderConfig()
                .setLoadingPlaceholder(R.drawable.loading)
                .setNotFoundPlaceholder(R.drawable.not_found)
                .setCache(new DoubleCache(this))
                .setThreadCount(4)
                .setLoadPolicy(new ReversePolicy());
        // 初始化
        SimpleImageLoader.getInstance().init(config);
    }
```     
2. call the displayImage to load bitmap.
```java
SimpleImageLoader.getInstance().displayImage(myImageView, "http://www.xxx/myimage.jpg");
```    


