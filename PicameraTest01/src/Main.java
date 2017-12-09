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
	
	public static final String uriBase = "<Your Azure Cognitive API URI>";
	public static final String subscriptionKey = "<Your Azure Subscription Key>";
	
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
                
                System.out.println("REST Response:\n");
                System.out.println(json.toString(2));
                
                HttpClient json_httpClient = HttpClientBuilder.create().build();
                
                HttpPost json_request = new HttpPost("<Your Web Server's Address");
                
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

}
