package com.mikhaellopez.androidwebserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

class AndroidWebServer extends NanoHTTPD {

    private String lastAction;
    private String lastCommand;
    private byte[] lastImage;

    private static final String kImageUploadPath = "/uploadImage";
    private static final String kLastCommandPath= "/lastCommand";
    private static final String kLastImagePath = "/lastImage";
    private static final String kUploadImageForm =
            "<form action=\"" + kImageUploadPath + "\" method=\"post\" enctype=\"multipart/form-data\">\n" +
            "    Select image to upload:\n" +
            "    <input type=\"file\" name=\"imageFile\" id=\"imageFile\">\n" +
            "    <input type=\"submit\" value=\"Upload Image\" name=\"submit\">\n" +
            "</form>";
    private long lastCommandUpdateTime;

    AndroidWebServer(int port) {
        super(port);
        lastAction = "";
        lastCommand = "none";
        lastImage = new byte[0];
        lastCommandUpdateTime = 0;
    }

    private void updateLastCommand(String lastCommand, String action) {
        this.lastAction = action;
        this.lastCommand = lastCommand;
        this.lastCommandUpdateTime = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        Method method = session.getMethod();
        if (session.getUri().equals(kLastCommandPath) &&
                session.getParms().containsKey("cmd") &&
                session.getParms().containsKey("action")) {
            String action = session.getParms().get("action").toLowerCase();
            if (action.equals("down") || action.equals("up")) {
                updateLastCommand(session.getParms().get("cmd"), session.getParms().get("action"));
                return newFixedLengthResponse("set last command: " + lastCommand);
            } else {
                return newFixedLengthResponse("command " + action + " is not one of down/up!");
            }
        }
        if (Method.POST.equals(method)) {
            if (session.getUri().equals(kImageUploadPath)) {
                try {
                    session.parseBody(files);
                    lastImage = IOUtil.readFile(files.get("imageFile"));
                    updateLastCommand("image", "");
                    return newFixedLengthResponse("uploaded image. size: " + lastImage.length);
                } catch (IOException | ResponseException e1) {
                    return newFixedLengthResponse("error uploading image: " + e1.getMessage());
                }
            }
            return newFixedLengthResponse("path " + session.getUri() + " not supported for POST.");
        }
        if (Method.GET.equals(method)) {
            if (session.getUri().equals(kLastImagePath)) {
                if (lastImage.length == 0) {
                    return newFixedLengthResponse("no image to send!");
                } else {
                    return newFixedLengthResponse(Response.Status.OK, MIME_HTML, new ByteArrayInputStream(lastImage), lastImage.length);
                }
            } else if (session.getUri().equals(kLastCommandPath)) {
                return newFixedLengthResponse(lastCommand + "," + lastAction + "," + lastCommandUpdateTime);
            } else {
                return newFixedLengthResponse(kUploadImageForm);
            }
        }
        return newFixedLengthResponse("method " + session.getMethod() + " not supported.");
    }


    private static class IOUtil {

        static byte[] readFile(String file) throws IOException {
            return readFile(new File(file));
        }

        static byte[] readFile(File file) throws IOException {
            // Open file
            RandomAccessFile f = new RandomAccessFile(file, "r");
            try {
                // Get and check length
                long longlength = f.length();
                int length = (int) longlength;
                if (length != longlength)
                    throw new IOException("File size >= 2 GB");
                // Read file and return data
                byte[] data = new byte[length];
                f.readFully(data);
                return data;
            } finally {
                f.close();
            }
        }
    }
}
