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
  //���ʱʱ��
  private static int TIME_OUT = 50000;
  //proxy����������˿�
  private static int ports = 8080;
  //proxy��������󳤶�
  private static int MAX_BUFFER = 1024;
  
  //��˸�����������in��out�������proxy��˵��
  //����cis�����Կͻ��˵Ľ���proxy��������
  InputStream cis = null, sis = null;
  BufferedReader cbr = null, sbr = null;
  OutputStream cos = null, sos = null;
  BufferedWriter cbw = null, sbw = null;
  
  
  //�ֱ��ʾproxy��Ϊ���������׽��֣����տͻ�����Ϣ����˽�csocket)��proxy������������׽��֣�proxy��Ϊ�ͻ��˵��׽���
  private Socket csocket = null;
  private static ServerSocket lsocket = null;
  private static Socket ssocket = null;
  
  //���Ļ���
  private String buffer = null;
  
  //��accpet���Ŀͻ����׽��ִ���һ����
  public MyHttpProxy(Socket cs) {
    try {
      csocket = cs;
      cis = csocket.getInputStream();
      cbr = new BufferedReader(new InputStreamReader(cis));//proxy��Ϊ���������ӿͻ��˻�ȡ����
      cos = csocket.getOutputStream();
      cbw = new BufferedWriter(new OutputStreamWriter(cos));//proxy��Ϊ����������ͻ��˷�������
      start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void run() {
    try {
      csocket.setSoTimeout(TIME_OUT);
      buffer = cbr.readLine();//��ȡ�ײ���
      System.out.println(buffer);
      
      String url = getRequestURL(buffer);//��ȡurl
      System.out.println(url);
      if(url == null) {
        return; //�����GET��POST��������������ʵ���ݲ������������
      }
      
      URL URL = new URL(url);//��url����ȡhost��port
      int port = URL.getPort();//�������ڶ˿ںţ���Ϊ-1����ʱʹ��Ĭ�϶˿ں�80
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
        sis = ssocket.getInputStream();//proxy��Ϊ�ͻ��ˣ��������Է�����������
        sbr = new BufferedReader(new InputStreamReader(sis));
        sos = ssocket.getOutputStream();//proxy��Ϊ�ͻ��ˣ����������������
        sbw = new BufferedWriter(new OutputStreamWriter(sos));
        
        String modifTime = null;
        if(modifTime == null) {
          while(!buffer.equals("")) {
            buffer += "\r\n";
            sbw.write(buffer);
            System.out.println("����������ͱ���"+buffer);
            buffer = cbr.readLine();
          }
          sbw.write("\r\n");//�����Ķ�������һ���б�ʾ����
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
      
      System.out.println("�����˼����׽���");
      while(true) {
        //��ÿһ�����ܵ������󴴽�һ���߳�
        Socket cs = lsocket.accept();
        
        System.out.println("���ܵ����󣬴������׽���");
        MyHttpProxy proxy = new MyHttpProxy(cs);
      }
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}