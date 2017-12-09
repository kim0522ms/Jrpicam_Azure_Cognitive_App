import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hopding.jrpicam.RPiCamera;
import com.hopding.jrpicam.enums.DRC;
import com.hopding.jrpicam.enums.Exposure;
import com.hopding.jrpicam.exceptions.FailedToRunRaspistillException;

public class Main {
	
	public static final String uriBase = "https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/ocr";
	public static final String subscriptionKey = "a1678fc3ebca45728db18c525bcad6a0";
	
	public static void main(String[] args) {
		try {
			//aaaa
			System.out.println("Capturing...");
			 
			Thread.sleep(2000);
			
			RPiCamera piCamera = new RPiCamera("/home/pi/appcenterTest/Pictures");
			
			piCamera.setWidth(640).setHeight(480)
		    .setBrightness(50)                // Adjust Camera's brightness setting.
		    .setDRC(DRC.OFF)
		    .setExposure(Exposure.AUTO)       // Set Camera's exposure.
		    .setQuality(75)
		    .setTimeout(2)                    // Set Camera's timeout.
		    .setAddRawBayer(true);
			
			//piCamera.takeStill("capture.jpg");
			
			
			BufferedImage image = ImageIO.read(piCamera.takeStill("capture.jpg"));
			
			File compressedImageFile = new File("/home/pi/appcenterTest/Pictures/compressedImage.jpg");
			OutputStream os = new FileOutputStream(compressedImageFile);
			
			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
			
			ImageWriter writer = (ImageWriter) writers.next();
			ImageOutputStream ios = ImageIO.createImageOutputStream(os);
			
			writer.setOutput(ios);
			
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(0.5f);
			
			writer.write(null, new IIOImage(image, null, null), param);
			os.close();
			ios.close();
			writer.dispose();
			
			//ImageIO.write(image, "jpg", new File("Image4.gif"));
			

		} catch (FailedToRunRaspistillException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// 촬영한 Snapshot 파일을 Binary Data로 바꿔서 Azure 서버에 전송
		HttpClient httpClient = new DefaultHttpClient();
		
		try
		{
			URIBuilder uriBuilder = new URIBuilder(uriBase);
			
			uriBuilder.setParameter("language", "unk");
			uriBuilder.setParameter("detectOrientation", "true");
			
			URI uri = uriBuilder.build();
			
			HttpPost request = new HttpPost(uri);
			
            request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
			
			File file = new File("/home/pi/appcenterTest/Pictures/compressedImage.jpg");
			
			HttpEntity entities = MultipartEntityBuilder.create()
					.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
					.addBinaryBody("file", file)
					.build();
			
			request.setEntity(entities);
			System.out.println("executing request " + request.getRequestLine());
			
            // JSON 형식으로 결과값 전달받음
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            // Response에 Entity가 정상적으로 입력되었을 경우
            if (entity != null)
            {
                String jsonString = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonString);
                
                JSONArray jsonArr = new JSONArray();
                jsonArr.put("35.166219");
                jsonArr.put("128.998042");
                
                json.put("coordinate", jsonArr);
                
                System.out.println("REST Response:\n");
                System.out.println(json.toString(2));
                
                HttpClient json_httpClient = HttpClientBuilder.create().build(); //Use this instead
                
                HttpPost json_request = new HttpPost("https://webapp-sillaserver.azurewebsites.net/op/JSON");
                
                StringEntity params =new StringEntity(json.toString());
                json_request.addHeader("content-type", "application/json");
                
                json_request.setEntity(params);
                
                HttpResponse json_response = json_httpClient.execute(json_request);
            }
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		/*
		try(ServerSocket server = new ServerSocket()){
            // 서버 초기화
            InetSocketAddress ipep = new InetSocketAddress(9999);
            server.bind(ipep);
             
            System.out.println("Waiting For Client...");
             
            //LISTEN 대기
            Socket client = server.accept();
            System.out.println("Connection");
             
            // send,reciever 스트림 받아오기
            // 자동 close
            try(OutputStream sender = client.getOutputStream();
                InputStream reciever = client.getInputStream();){
            	
                //클라이언트로부터 메시지 받기
                byte[] data = new byte[11];
                reciever.read(data, 0, data.length);
                 
                //수신 메시지 출력
                String message = new String(data);
                message = message.trim();
                
                // 클라이언트로부터 받은 메세지가 촬영일 경우
                if (message.equals("shot"))
                {
                	
                	// TODO : 촬영 관련 코드 작성 
                	try {
            			RPiCamera piCamera = new RPiCamera("/home/pi/appcenterTest/Pictures");
            			
            			piCamera.setWidth(640).setHeight(480)
            		    .setBrightness(50)                // Adjust Camera's brightness setting.
            		    .setDRC(DRC.OFF)
            		    .setExposure(Exposure.AUTO)       // Set Camera's exposure.
            		    .setQuality(75)
            		    .setTimeout(2)                    // Set Camera's timeout.
            		    .setAddRawBayer(true);
            			
            			//piCamera.takeStill("Image3.jpg");
            			
            			BufferedImage image = ImageIO.read(piCamera.takeStill("capture.jpg"));
            			
            			File compressedImageFile = new File("/home/pi/appcenterTest/Pictures/compressedImage.jpg");
            			OutputStream os = new FileOutputStream(compressedImageFile);
            			
            			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            			
            			ImageWriter writer = (ImageWriter) writers.next();
            			ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            			
            			writer.setOutput(ios);
            			
            			ImageWriteParam param = writer.getDefaultWriteParam();
            			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            			param.setCompressionQuality(0.5f);
            			
            			writer.write(null, new IIOImage(image, null, null), param);
            			os.close();
            			ios.close();
            			writer.dispose();
            			
            			//ImageIO.write(image, "jpg", new File("Image4.gif"));
            			

            		} catch (FailedToRunRaspistillException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		} catch (IOException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		} catch (InterruptedException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		}
            		
            		// 촬영한 Snapshot 파일을 Binary Data로 바꿔서 Azure 서버에 전송
            		HttpClient httpClient = new DefaultHttpClient();
            		
            		try
            		{
            			URIBuilder uriBuilder = new URIBuilder(uriBase);
            			
            			uriBuilder.setParameter("language", "unk");
            			uriBuilder.setParameter("detectOrientation", "true");
            			
            			URI uri = uriBuilder.build();
            			
            			HttpPost request = new HttpPost(uri);
            			
                        request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
            			
            			File file = new File("/home/pi/appcenterTest/Pictures/compressedImage.jpg");
            			
            			HttpEntity entities = MultipartEntityBuilder.create()
            					.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            					.addBinaryBody("file", file)
            					.build();
            			
            			request.setEntity(entities);
            			System.out.println("executing request " + request.getRequestLine());
            			
                        // JSON 형식으로 결과값 전달받음
                        HttpResponse response = httpClient.execute(request);
                        HttpEntity entity = response.getEntity();

                        // Response에 Entity가 정상적으로 입력되었을 경우
                        if (entity != null)
                        {
                            String jsonString = EntityUtils.toString(entity);
                            JSONObject json = new JSONObject(jsonString);
                            
                            JSONArray jsonArr = new JSONArray();
                            jsonArr.put("35.168647");
                            jsonArr.put("128.994492");
                            
                            json.put("coordinate", jsonArr);
                            
                            System.out.println("REST Response:\n");
                            System.out.println(json.toString(2));
                            
                            HttpClient json_httpClient = HttpClientBuilder.create().build(); //Use this instead
                            
                            HttpPost json_request = new HttpPost("http://172.17.19.49:8081/Silla_Server/op/JSON");
                            
                            StringEntity params =new StringEntity(json.toString());
                            json_request.addHeader("content-type", "application/json");
                            
                            json_request.setEntity(params);
                            
                            HttpResponse json_response = json_httpClient.execute(json_request);
                        }
            		}
            		catch (Exception e)
            		{
            			System.out.println(e.getMessage());
            		}
                }
                else if (message.equals("shutdown"))
                {
                	// TODO : 프로그램 종료
                	System.out.println("Recieved Message : " + message);
                	System.out.println("Exit Program...");
                	return;
                }
                else
                {
                	// TODO : 예외처리
                	System.out.println("Unknown Message : " + message);
                }
            }
        }catch(Throwable e){
            e.printStackTrace();
    }
    */
		/*
		try(ServerSocket server = new ServerSocket()){
            // 서버 초기화
            InetSocketAddress ipep = new InetSocketAddress(10500);
            server.bind(ipep);
             
            System.out.println("Waiting For Client...");
             
            //LISTEN 대기
            Socket client = server.accept();
            System.out.println("Connectted From Client!");
            
            // send,reciever 스트림 받아오기
            // 자동 close
            try(OutputStream sender = client.getOutputStream();
                InputStream reciever = client.getInputStream();){
            	
                //클라이언트로부터 메시지 받기
                byte[] data = new byte[11];
                reciever.read(data, 0, data.length);
                 
                //수신 메시지 출력
                String message = new String(data);
                message = message.trim();
                
                // 클라이언트로부터 받은 메세지가 촬영일 경우
                if (message.equals("shot"))
                {
                	// TODO : 촬영 관련 코드 작성 
                	try {
            			RPiCamera piCamera = new RPiCamera("/home/pi/appcenterTest/Pictures");
            			
            			piCamera.setWidth(1280).setHeight(720)
            		    .setBrightness(50)                // Adjust Camera's brightness setting.
            		    .setDRC(DRC.OFF)
            		    .setExposure(Exposure.AUTO)       // Set Camera's exposure.
            		    .setQuality(75)
            		    .setTimeout(2)                    // Set Camera's timeout.
            		    .setAddRawBayer(true);
            			
            			//piCamera.takeStill("Image3.jpg");
            			
            			BufferedImage image = ImageIO.read(piCamera.takeStill("capture.jpg"));
            			
            			File compressedImageFile = new File("/home/pi/appcenterTest/Pictures/compressedImage.jpg");
            			OutputStream os = new FileOutputStream(compressedImageFile);
            			
            			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            			
            			ImageWriter writer = (ImageWriter) writers.next();
            			ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            			
            			writer.setOutput(ios);
            			
            			ImageWriteParam param = writer.getDefaultWriteParam();
            			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            			param.setCompressionQuality(0.5f);
            			
            			writer.write(null, new IIOImage(image, null, null), param);
            			os.close();
            			ios.close();
            			writer.dispose();
            			
            			//ImageIO.write(image, "jpg", new File("Image4.gif"));
            			

            		} catch (FailedToRunRaspistillException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		} catch (IOException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		} catch (InterruptedException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		}
            		
            		// 촬영한 Snapshot 파일을 Binary Data로 바꿔서 Azure 서버에 전송
            		HttpClient httpClient = new DefaultHttpClient();
            		
            		try
            		{
            			URIBuilder uriBuilder = new URIBuilder(uriBase);
            			
            			uriBuilder.setParameter("language", "unk");
            			uriBuilder.setParameter("detectOrientation", "true");
            			
            			URI uri = uriBuilder.build();
            			
            			HttpPost request = new HttpPost(uri);
            			
                        request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
            			
            			File file = new File("/home/pi/appcenterTest/Pictures/compressedImage.jpg");
            			
            			HttpEntity entities = MultipartEntityBuilder.create()
            					.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            					.addBinaryBody("file", file)
            					.build();
            			
            			request.setEntity(entities);
            			System.out.println("executing request " + request.getRequestLine());
            			
                        // JSON 형식으로 결과값 전달받음
                        HttpResponse response = httpClient.execute(request);
                        HttpEntity entity = response.getEntity();

                        // Response에 Entity가 정상적으로 입력되었을 경우
                        if (entity != null)
                        {
                            String jsonString = EntityUtils.toString(entity);
                            JSONObject json = new JSONObject(jsonString);
                            
                            JSONArray jsonArr = new JSONArray();
                            jsonArr.put("35.168647");
                            jsonArr.put("128.994492");
                            
                            json.put("coordinate", jsonArr);
                            
                            System.out.println("REST Response:\n");
                            System.out.println(json.toString(2));
                            
                            HttpClient json_httpClient = HttpClientBuilder.create().build(); //Use this instead
                            
                            HttpPost json_request = new HttpPost("http://192.168.43.7:8081/Silla_Server/op/JSON");
                            
                            StringEntity params =new StringEntity(json.toString());
                            json_request.addHeader("content-type", "application/json");
                            
                            json_request.setEntity(params);
                            
                            HttpResponse json_response = json_httpClient.execute(json_request);
                        }
            		}
            		catch (Exception e)
            		{
            			System.out.println(e.getMessage());
            		}
                }
                else if (message.equals("shutdown"))
                {
                	// TODO : 프로그램 종료
                	System.out.println("Recieved Message : " + message);
                	System.out.println("Exit Program...");
                	return;
                }
                else
                {
                	// TODO : 예외처리
                	System.out.println("Unknown Message : " + message);
                }
            }
        }catch(Throwable e){
            e.printStackTrace();
    }
    */
	}

}
