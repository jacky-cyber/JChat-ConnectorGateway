##web im的分析
***
###前端到后台连接

####1、flash
> 在Html页面中嵌入一个使用了XML Socket类的Flash程序，通过js与其交互实现和服务器的长连接通信。

####2、Comet(sockjs)
> 该技术要求客户端浏览器支持HTTP/1.1，HTTP/1.1协议定义的一些技术规范被应用与实践中，例如持久连接、chunked transfer-encoding、pipeline等。
对于一些受限的客户端（移动终端等）可能不能很好的支持像chunked（块传输）、pipeline等特性。

##### 两种comet模型
> 
a、基于 AJAX 的长轮询（long-polling）方式。
>
使用 AJAX 实现“服务器推”与传统的 AJAX 应用不同之处在于：
* 服务器端会阻塞请求直到有数据传递或超时才返回。
* 客户端 JavaScript 响应处理函数会在处理完服务器返回的信息后，再次发出请求，重新建立连接。
* 当客户端处理接收的数据、重新建立连接时，服务器端可能有新的数据到达；这些信息会被服务器端保存直到客户端重新建立连接     ，客户端会一次把当前服务器端所有的信息取回。
>
b、基于 Iframe 及 htmlfile 的流（streaming）方式。
>
iframe 是很早就存在的一种 HTML 标记， 通过在 HTML 页面里嵌入一个隐蔵帧，然后将这个隐蔵帧的 SRC 属性设为对一个长连接的请求，服务器端就能源源不断地往客户端输入数据。
* 在 iframe 方案的客户端，iframe 服务器端并不返回直接显示在页面的数据，而是返回对客户端 Javascript 函数的调用，如“<script type="text/javascript">js_func(“data from server ”)</script>”。服务器端将返回的数据作为客户端 JavaScript 函数的参数传递；客户端浏览器的 Javascript 引擎在收到服务器返回的 JavaScript 调用时就会去执行代码。
* 每次数据传送不会关闭连接，连接只会在通信出现错误时，或是连接重建时关闭（一些防火墙常被设置为丢弃过长的连接， 服务器端可以设置一个超时时间， 超时后通知客户端重新建立连接，并关闭原来的连接）。
* 使用 iframe 请求一个长连接有一个很明显的不足之处：IE、Morzilla Firefox 下端的进度栏都会显示加载没有完成，而且 IE 上方的图标会不停的转动，表示加载正在进行。Google 的天才们使用一个称为“htmlfile”的 ActiveX 解决了在 IE 中的加载显示问题，并将这种方法用到了 gmail+gtalk 产品中。Alex Russell 在 “What else is burried down in the depth's of Google's amazing JavaScript?”文章中介绍了这种方法。Zeitoun 网站提供的 comet-iframe.tar.gz，封装了一个基于 iframe 和 htmlfile 的 JavaScript comet 对象，支持 IE、Mozilla Firefox 浏览器，可以作为参考。

####3、Nodejs(socket.io)
> 只需关注在需要的事件点上即可

####4、WebSocket(sockjs)
> WebSocket protocol 是HTML5一种新的协议。它实现了浏览器与服务器全双工通信(full-duplex)。
Chrome    Supported in version 4+
Firefox   Supported in version 4+
IE        Supported in version 10+
Opera     Supported in version 10+
Safari    Supported in version 5+
Spring Framework4

### JPush到IM后台的连接
??

### 目前实现
> 1、Sockejs + spring4  (websocket + ajax long polling)

>  2、socket.io.js + netty (webscoket + ajax long polling)
