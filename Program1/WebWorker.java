/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;


public class WebWorker implements Runnable {

   private Socket socket;
   private String URL = "";

   /**
   * Constructor: must have a valid open socket
   **/
   public WebWorker(Socket s){
      socket = s;
   }

   /**
   * Worker thread starting point. Each worker handles just one HTTP 
   * request and then returns, which destroys the thread. This method
   * assumes that whoever created the worker created it with a valid
   * open socket object.
   **/
   public void run(){
      System.err.println("Handling connection...");
      try {
        InputStream  is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        readHTTPRequest(is);
        writeHTTPHeader(os,"text/html");
        writeContent(os, URL);
        os.flush();
        socket.close();
      } catch (Exception e) {
            System.err.println("Output error: "+e);
         }
      System.err.println("Done handling connection.");
      return;
   }

   /**
   * Read the HTTP request header.
   **/
   private void readHTTPRequest(InputStream is) {
      String line;
      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      while (true) {
         try {
            while (!r.ready()) Thread.sleep(1);
            line = r.readLine();
            System.err.println("Request line: ("+line+")");
            if (line.length()==0) break;
            if (line.substring(0,3).equals("GET")){
               URL = line.substring(5).split(" ")[0];
            }

         } catch (Exception e) {
            System.err.println("Request error: "+e);
            break;
         }
      }
      return;
   }

   /**
   * Write the HTTP header lines to the client network connection.
   * @param os is the OutputStream object to write to
   * @param contentType is the string MIME content type (e.g. "text/html")
   **/
   private void writeHTTPHeader(OutputStream os, String contentType) throws Exception {

      //Get URL and replace all '/' with '\'
      String tempURL = URL;
      tempURL = tempURL.replace("/", "\\");
      File someFile = new File(tempURL);

      Date d = new Date();
      DateFormat df = DateFormat.getDateTimeInstance();
      df.setTimeZone(TimeZone.getTimeZone("GMT"));

      // Request/Error response codes
      if(someFile.isFile()){
         os.write("HTTP/1.1 200 OK\n".getBytes());
      } else {
         os.write("HTTP/1.1 404 Not Found\n".getBytes());
      }

      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Tony's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
      return;
   } // end writeHTTP Header

   /**
   * Write the data content to the client network connection. This MUST
   * be done after the HTTP header has been written out.
   * @param os is the OutputStream object to write to
   **/
   private void writeContent(OutputStream os, String someURL) throws Exception {

      someURL = someURL.replace("/", "\\"); 
      File someFile = new File(someURL);

      SimpleDateFormat dateFormat = new SimpleDateFormat("mm/dd/yyyy");
      Date date = new Date();

      if(!someURL.contains("favicon.cio")){

         if(someFile.isFile()){
            byte[] encodedFile = Files.readAllBytes(Paths.get(someURL));
            
            String fileContents = new String(encodedFile, StandardCharsets.UTF_8);

            // Replacing date tag
            String dateTag = dateFormat.format(date);
            fileContents = fileContents.replace("<cs371date>", dateTag);

            // Replacing server tag
            String serverTag = "Tony's super cool and working server";
            fileContents = fileContents.replace("<cs371server", serverTag);

            os.write(fileContents.getBytes());

         }
         else{

            // Error page
            String error404 = "error404.html";

            byte[] encodedFile = Files.readAllBytes(Paths.get(error404));
            String fileContents = new String(encodedFile, StandardCharsets.UTF_8);

            os.write("<center> Error! Something isn't right here. </center>".getBytes());
            os.write(fileContents.getBytes());
         }
      }
   } // end writeContent

} // end class





