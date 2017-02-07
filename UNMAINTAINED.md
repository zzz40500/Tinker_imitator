

![Tinker_imitator.png](https://raw.githubusercontent.com/zzz40500/Tinker_imitator/master/screenshot/img.png)  

##[原理: 微信热更新方案](http://mp.weixin.qq.com/s?__biz=MzAwNDY1ODY2OQ==&mid=2649286306&idx=1&sn=d6b2865e033a99de60b2d4314c6e0a25&scene=0#wechat_redirect)
简单的讲: 增量更新  
[Tinker_imitator地址](https://github.com/zzz40500/Tinker_imitator)
 

      电脑:mac  
      编译工具:as & intellj  
      gradle版本 com.android.tools.build:gradle:2.1.2  
      android版本:6.0
##准备动作:
###1. 安装bsdiff:
mac 端命令:
```
 brew install bsdiff
```
linux端命令:  
```
brew install bsdiff
```
Windows:  
使用cygwin安装  
然后将bsdiff 安装的位置写入local.properties  
![Paste_Image.png](http://upload-images.jianshu.io/upload_images/166866-f9936846f287b6a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
mac 端不写.默认为/usr/local/bin/bsdiff  
linux 和Windows要写.  
>注意  我只测试了mac 的使用.  

### 2. 安装ide插件.  
[Tinker-Plugin地址](https://github.com/zzz40500/Tinker_imitator/blob/master/plugin/Tinker-Plugin.zip)  
安装方式:[这篇文章](https://github.com/zzz40500/GsonFormat)第2种方式.

##3. 编译运行.  
这里暂时不支持使用instant run 的情况. 所以你要关闭instant run   
关闭方式:自行google|bing  
第一次编译:  
![第一次运行](http://upload-images.jianshu.io/upload_images/166866-de367ac222ea7518.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
编译完成会产生几个文件:  

![产生的文件.png](http://upload-images.jianshu.io/upload_images/166866-9d080c1b95d2e408.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
然后修改代码:    
打补丁包:  

![补丁包运行.png](http://upload-images.jianshu.io/upload_images/166866-3b7319b26baee7c7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
会有下列产物:  

![patch产物.png](http://upload-images.jianshu.io/upload_images/166866-cf7b5fa7772f962c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
patchclasses.dex 是生成的patch dex. 如果你连接手机的话,ide插件会帮你push 到手机的/sdcard/hot/中    
classes和class2 分别对应apk 中的classes.dex和classes2.dex.  
log 是运行日志. 你可以直接使用日志中的命令执行,而不使用我提供的插件     

##查看效果:  
方式一: app 重启  
方式二: 点击app 的内部的热修复按钮.  

##4. 不足:  
1. 热修复. 需要重启  
* 只是代码级别的热修复. 不支持资源的替换.修改代码的时候不能新增资源id.  
* 如果改变了两个dex里面的东西的话,那么占得内存就有点大了 


##5. todo:
1. 签名验证;
* gradle配置热修复
* 支持instant run 
* 包裹dex.而不是直接传递dex;
* patch版本控制;
* 部分情况下不用重启app就能生效;
* 更智能的dex管理;
* 安全模式.防止因为错误的patch导致的app启动不起来;
* 更好的差分算法;
* 资源更新;

##6. 尾巴  
最近[阿宅](https://github.com/markzhai)开了个QQ实践群(568863373)，欢迎大家进来玩耍，也可以关注我们的公众号：**魔都三帅**  

![Paste_Image.png](http://upload-images.jianshu.io/upload_images/166866-48ed6363e2282f7f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

特别感谢:  
https://github.com/jasonross/Nuwa  
https://github.com/ceabie/DexKnifePlugin  
https://github.com/brok1n/androidBsdiffUpdate


      
