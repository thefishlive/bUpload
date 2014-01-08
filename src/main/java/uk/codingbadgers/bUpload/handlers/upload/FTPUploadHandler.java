package uk.codingbadgers.bUpload.handlers.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import com.google.common.base.Splitter;

import static net.minecraft.client.resources.I18n.*;

import uk.codingbadgers.bUpload.Screenshot;
import uk.codingbadgers.bUpload.handlers.ConfigHandler;
import uk.codingbadgers.bUpload.handlers.MessageHandler;
import uk.codingbadgers.bUpload.handlers.auth.FTPAuthHandler;
import uk.codingbadgers.bUpload.handlers.auth.FTPAuthHandler.FTPUserData;

public class FTPUploadHandler extends UploadHandler {

    private static final int BINARY_TRANSFER_MODE = 0x02;
    
    private static final Minecraft minecraft = Minecraft.getMinecraft();
    
    public FTPUploadHandler(Screenshot screen) {
        super(screen);
    }

    @Override
    public boolean run(Screenshot screen) {
        FTPClient client = new FTPClient();
        
        try {
        	FTPAuthHandler auth = FTPAuthHandler.getInstance();
        	FTPUserData data = auth.getUserData();
            client.connect(data.host, data.port);
            
            int reply = client.getReplyCode();
            
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException(getStringParams("image.upload.ftp.cannotconnect", reply));
            }
            
            if (!client.login(data.username, new String(data.password))) {
                client.logout();
                throw new IOException(getStringParams("image.upload.ftp.incorrectlogin", auth.getUserData().username));
            }
            
            client.setListHiddenFiles(false);
            client.setFileType(BINARY_TRANSFER_MODE);
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(screen.image,ConfigHandler.IMAGE_FORMAT, os); 
            InputStream fis = new ByteArrayInputStream(os.toByteArray());
            
            String path = ConfigHandler.formatImagePath(minecraft);
            path = path.substring(0, path.lastIndexOf(File.separatorChar));
            navToDir(client, path);
            
            boolean uploaded = client.storeFile(ConfigHandler.SAVE_DATE_FORMAT.format(new Date()).toString() + ".png", fis);
            
            if (uploaded) {
            	ChatComponentTranslation message = new ChatComponentTranslation("image.upload.success");
                ChatComponentText url = new ChatComponentText("FTP server");
                url.func_150255_a(new ChatStyle().func_150238_a(EnumChatFormatting.GOLD));
                message.func_150257_a(url);
                
                MessageHandler.sendChatMessage(message);
            } else {
                MessageHandler.sendChatMessage("image.upload.fail", "FTP server", client.getReplyString());
            }
        } catch (Exception e) {
            MessageHandler.sendChatMessage("image.upload.fail", "FTP server", e.getMessage());
            e.printStackTrace();
        } finally {
            if (client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return false;
    }

    private void navToDir(FTPClient client, String dir) throws IOException {
    	Splitter splitter = Splitter.on(File.separatorChar).omitEmptyStrings();
    	
    	for (String sting : splitter.split(dir)) {
	        if (!client.changeWorkingDirectory(sting)) {
	            client.makeDirectory(sting);
	            client.changeWorkingDirectory(sting);
	        }
    	}
    }

}
