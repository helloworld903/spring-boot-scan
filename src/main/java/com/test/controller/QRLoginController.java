package com.test.controller;

import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.hui.quick.plugin.base.DomUtil;
import com.github.hui.quick.plugin.base.constants.MediaType;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeGenWrapper;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeOptions;
import com.google.zxing.WriterException;
import com.test.common.utils.IpUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Controller
public class QRLoginController {
	@Value("${server.port}")
	private int port;
	
	@GetMapping("login")
	public String qr(Map<String,Object>data) throws IOException, WriterException {
		//下列函数返回的是一个随机的UUID字符串，格式为8-4-4-4-12的32个字符组成的字符串。
		//UUID是一个标准化的标识符，在不同的系统和应用中都可以保证唯一性，通常用于分布式系统、数据库主键等场景。
		String id=UUID.randomUUID().toString();
		
		
		String ip=IpUtils.getInnerIp();
		if(ip==null)ip=IpUtils.DEFAULT_IP;
		
		String pref="http://"+ip+":"+port+"/";
		data.put("redirect", pref+"home");
		data.put("subscribe", pref+"subscribe?id="+id);
		
		String qrUrl=pref+"scan?id="+id;
		//生成二维码
		String qrCode=QrCodeGenWrapper.of(qrUrl).setW(200).setDrawPreColor(Color.green).setDrawStyle(QrCodeOptions.DrawStyle.CIRCLE).asString();
		data.put("qrcode", DomUtil.toDomSrc(qrCode, MediaType.ImageJpg));
		
		
		Iterator iter =data.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry entry=(Map.Entry)iter.next();
			String key=(String) entry.getKey();
			String val=(String) entry.getValue();
			System.out.printf("%s : %s\n", key,val);
		}
		
		
		return "login";
	}
	
	//SSE服务器发送事件，该场景下，客户端发起请求，连接一直保持，服务端有数据就可以返回数据给客户端，这个返回可以是多次间隔的方式
	// 新建一个容器，保存连接，用于输出返回
	private Map<String,SseEmitter>cache=new ConcurrentHashMap<>();
	
	@GetMapping(path="subscribe")
	public SseEmitter subscribe(String id,HttpServletResponse response) throws IOException{
		//设置五分钟的超时时间
		System.out.println("this is subscribe: "+id);
		
		response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
		SseEmitter sseEmitter=new SseEmitter(5*60*1000L);
		cache.put(id, sseEmitter);
		sseEmitter.onTimeout(()->cache.remove(id));
		sseEmitter.onError((e)->cache.remove(id));
		//监听器关闭emitter
		
		System.out.printf("sseEmitter: %s\n", sseEmitter.toString());
		return sseEmitter;
	}
	
	@GetMapping("scan")
	public String scan(Model model,HttpServletRequest request) throws IOException {
		String id=request.getParameter("id");
		SseEmitter sseEmitter=cache.get(request.getParameter("id"));
		
		if(sseEmitter!=null) {
			sseEmitter.send("scan");
		}
		
		String url="http://"+IpUtils.getInnerIp()+":"+port+"/accept?id="+id;
		model.addAttribute("url",url);
		return "scan";
	}
	
	@ResponseBody
	@GetMapping("accept")
	public String accept(String id,String token) throws IOException {
		SseEmitter sseEmitter=cache.get(id);
		if(sseEmitter!=null) {
			//发送登录成功事件
			sseEmitter.send("login#qrlogin="+token);
			sseEmitter.complete();
			cache.remove(id);
		}
		return "登录成功: "+token;
	}
	
	@ResponseBody
	@GetMapping(path= {"home",""})
	public String home(HttpServletRequest request) {
		Cookie[]cookies=request.getCookies();
		if(cookies==null||cookies.length==0) {
			return "未登录！";
		}
		
		Optional<Cookie>cookie=Stream.of(cookies).filter(s->s.getName().equalsIgnoreCase("qrlogin")).findFirst();
		return cookie.map(cookie1->"欢迎进入首页: "+cookie1.getValue()).orElse("未登录！");
	}
}
