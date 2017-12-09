# Jrpicam_Azure_Cognitive_App

This program extracts text from photos taken using Picamera of Raspberry pi and sends it to Server.

Before using this program, you need the Azure subscription and the Computer Vision API.
In addition, the extracted text data is sent in HTTP POST format, so Web Server to handle this is also required.

The structure of the program is as follows.
1. Use the library to get a snapshot of Picamera.
2. Compress the image to transfer files to the Azure Computer Vision API.
(Because the maximum supportable size of the API is 4MB).
3. When an image is sent in POST format, the API extracts the character and returns it in JSON format.
4. This program sends the received JSON-formatted text back to my web server.
You can also process the JSON data from the API in Raspberry Pi and send it to the server.

If you are looking for documentation on the Jrpicam library, check the link below.

>JRPiCam Wiki https://github.com/Hopding/JRPiCam/wiki
