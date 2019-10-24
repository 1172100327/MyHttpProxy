package main;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;


public class MyHttpProxy extends Thread{
  //最大超时时长
  private static int TIME_OUT = 50000;
  //proxy监听浏览器端口
  private static int ports = 8080;
  //proxy缓冲区最大长度
  private static int MAX_BUFFER = 1024;
  
  //这八个变量命名中in和out是相对于proxy来说的
  //例如cis是来自客户端的进入proxy的数据流
  InputStream cis = null, sis = null;
  BufferedReader cbr = null, sbr = null;
  OutputStream cos = null, sos = null;
  BufferedWriter cbw = null, sbw = null;
  
  
  //分别表示proxy作为服务器的套接字（接收客户端信息，因此叫csocket)，proxy监听浏览器的套接字，proxy作为客户端的套接字
  private Socket csocket = null;
  private static ServerSocket lsocket = null;
  private static Socket ssocket = null;
  
  //报文缓存
  private String buffer = null;
  
  //用accpet到的客户端套接字创建一个类
  public MyHttpProxy(Socket cs) {
    try {
      csocket = cs;
      cis = csocket.getInputStream();
      cbr = new BufferedReader(new InputStreamReader(cis));//proxy作为服务器，从客户端获取数据
      cos = csocket.getOutputStream();
      cbw = new BufferedWriter(new OutputStreamWriter(cos));//proxy作为服务器，向客户端发送数据
      start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void run() {
    try {
      csocket.setSoTimeout(TIME_OUT);
      buffer = cbr.readLine();//读取首部行
      System.out.println(buffer);
      
      String url = getRequestURL(buffer);//提取url
      System.out.println(url);
      if(url == null) {
        return; //处理除GET和POST外的其他情况，本实验暂不考虑其他情况
      }
      
      URL URL = new URL(url);//从url中提取host和port
      int port = URL.getPort();//若不存在端口号，则为-1，此时使用默认端口号80
      String host = URL.getHost();
      System.out.println(host + ":" + port);
      
      if(port == -1) {
        ssocket = new Socket(host, 80);
      }
      else {
        ssocket = new Socket(host, port);
      }
      if(ssocket != null) {
        ssocket.setSoTimeout(TIME_OUT);
        sis = ssocket.getInputStream();//proxy作为客户端，接收来自服务器的数据
        sbr = new BufferedReader(new InputStreamReader(sis));
        sos = ssocket.getOutputStream();//proxy作为客户端，向服务器发送数据
        sbw = new BufferedWriter(new OutputStreamWriter(sos));
        
        String modifTime = null;
        if(modifTime == null) {
          while(!buffer.equals("")) {
            buffer += "\r\n";
            sbw.write(buffer);
            System.out.println("向服务器发送报文"+buffer);
            buffer = cbr.readLine();
          }
          sbw.write("\r\n");//将报文读完后，添加一空行表示结束
          sbw.flush();
        }
        
        int length = 0;
        byte[] bytes = new byte[MAX_BUFFER];
        while((length = sis.read(bytes)) > 0){
          cos.write(bytes, 0, length);
        }
        cos.flush();
        cbw.write("\r\n");
      }
    } catch (SocketException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public String getRequestURL(String buffer) {
    String url = null;
    String[] parts = buffer.split(" ");
    if(parts[0].equals("GET") || parts[0].equals("POST")) {
      url = parts[1];
    }
    return url;
  }
  
  public static void main(String[] args) {
    try {
      lsocket = new ServerSocket(ports);
//      lsocket.setSoTimeout(10000);
      
      System.out.println("创建了监听套接字");
      while(true) {
        //对每一个接受到的请求创建一个线程
        Socket cs = lsocket.accept();
        
        System.out.println("接受到请求，创建了套接字");
        MyHttpProxy proxy = new MyHttpProxy(cs);
      }
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}