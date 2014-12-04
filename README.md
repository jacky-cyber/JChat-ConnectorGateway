##web im的分析
***
###服务端推方案
####1、BOSH
  BOSH技术能够同时减小网络带宽和减小客户端响应的时间。其方案是对客户端的请求连接管理器不给于返回直到数据已经就绪，当客户端收取连接管理器返回的数据会向连接管理器发送下一个请求，于是连接管理器总是保持着一个客户端的请求，当服务器端数据就绪的时候，可以将数据封装在请求的响应包中，“推送”给客户端。

  如果双向连接长时间没有数据，连接管理器负责给客户端发送一个空包，空包触发客户端发送新的请求，连接管理器通过这种机制判断连接是否已经中断，由于BOSH不是轮询的机制，带宽消耗比标准的TCP连接大不了多少。
