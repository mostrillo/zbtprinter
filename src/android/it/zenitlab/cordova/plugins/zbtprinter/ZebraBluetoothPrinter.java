package it.zenitlab.cordova.plugins.zbtprinter;

import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

public class ZebraBluetoothPrinter extends CordovaPlugin {

    private static final String LOG_TAG = "ZebraBluetoothPrinter";
    //String mac = "AC:3F:A4:52:73:C4";//00:07:80:2D:DE:38
	
    public ZebraBluetoothPrinter() {
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("print")) {
            try {
                String msg = args.getString(0);
                String mac = args.getString(1);
                sendData(callbackContext, msg, mac);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else {
	    if (action.equals("file")) {
		try {
		    String filepath = args.getString(0);
		    String mac = args.getString(1);
		    sendFile(callbackContext, filepath, mac);
		} catch (IOException e) {
		    Log.e(LOG_TAG, e.getMessage());
		    e.printStackTrace();
		}
		return true;
	    } else {
	    	if (action.equals("image")) {
		    try {
		        JSONArray labels = args.getJSONArray(0);
                        String mac = args.getString(1);
                        sendImage(callbackContext, labels, mac);
		    } catch (IOException e) {
		        Log.e(LOG_TAG, e.getMessage());
		        e.printStackTrace();
		    }
		    return true;
	        }
	    }
	}
	    
        return false;
    }

    /*
     * This will send data to be printed by the bluetooth printer
     */
    void sendData(final CallbackContext callbackContext, final String msg, final String mac) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

                    // Verify the printer is ready to print
                    if (isPrinterReady(thePrinterConn)) {

                        // Open the connection - physical connection is established here.
                        thePrinterConn.open();

                        // Send the data to printer as a byte array.
//                        thePrinterConn.write("^XA^FO0,20^FD^FS^XZ".getBytes());
                        thePrinterConn.write(msg.getBytes());


                        // Make sure the data got to the printer before closing the connection
                        Thread.sleep(500);

                        // Close the insecure connection to release resources.
                        thePrinterConn.close();
                        callbackContext.success("Stampa terminata");
                    } else {
						callbackContext.error("printer is not ready");
					}
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }
	
	/*
     * This will send file to be printed by the bluetooth printer
     */
    void sendFile(final CallbackContext callbackContext, final String filepath, final String mac) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

                    // Verify the printer is ready to print
                    if (isPrinterReady(thePrinterConn)) {

                        // Open the connection - physical connection is established here.
                        thePrinterConn.open();
			    
			ZebraPrinter printer = ZebraPrinterFactory.getInstance(thePrinterConn);
			String printerLanguage = SGD.GET("device.languages", thePrinterConn);
			if (!printerLanguage.contains("zpl")) {
			     SGD.SET("device.languages", "hybrid_xml_zpl", thePrinterConn);
			}
			printer.sendFileContents(filepath);    
			    
                        // Make sure the data got to the printer before closing the connection
                        Thread.sleep(500);

                        // Close the insecure connection to release resources.
                        thePrinterConn.close();
                        callbackContext.success("Stampa terminata");
                    } else {
						callbackContext.error("printer is not ready");
					}
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }
	
    void sendImage(final CallbackContext callbackContext, final JSONArray labels, final String mac) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
		    // Instantiate insecure connection for given Bluetooth MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);

		    // Verify the printer is ready to print
                    if (isPrinterReady(thePrinterConn)) {
			 // Open the connection - physical connection is established here.
                         thePrinterConn.open();
			    
			 ZebraPrinter printer = ZebraPrinterFactory.getInstance(thePrinterConn);   
			    
			 ZebraPrinterLinkOs zebraPrinterLinkOs = ZebraPrinterFactory.createLinkOsPrinter(printer);

			 for (int i = labels.length() - 1; i >= 0; i--) {
			    String base64Image = labels.get(i).toString();
			    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
			    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
			    ZebraImageAndroid zebraimage = new ZebraImageAndroid(decodedByte);

			    //Lengte van het label eerst instellen om te kleine of te grote afdruk te voorkomen
			    if (zebraPrinterLinkOs != null && i == labels.length() - 1) {
				String currentLabelLength = zebraPrinterLinkOs.getSettingValue("zpl.label_length");
				if (!currentLabelLength.equals(String.valueOf(zebraimage.getHeight()))) {
				    zebraPrinterLinkOs.setSetting("zpl.label_length", zebraimage.getHeight() + "");
				}
			    }

			    if (zebraPrinterLinkOs != null) {
				printer.printImage(zebraimage, 150, 0, zebraimage.getWidth(), zebraimage.getHeight(), false);
			    } else {
				Log.d(LOG_TAG, "Storing label on printer...");
				printer.storeImage("wgkimage.pcx", zebraimage, -1, -1);
				String cpcl = "! 0 200 200 ";
				cpcl += zebraimage.getHeight();
				cpcl += " 1\r\n";
				cpcl += "PW 750\r\nTONE 0\r\nSPEED 6\r\nSETFF 203 5\r\nON - FEED FEED\r\nAUTO - PACE\r\nJOURNAL\r\n";
				cpcl += "PCX 150 0 !<wgkimage.pcx\r\n";
				cpcl += "FORM\r\n";
				cpcl += "PRINT\r\n";
				thePrinterConn.write(cpcl.getBytes());
			    }
			 }

			 //Voldoende wachten zodat label afgeprint is voordat we een nieuwe printer-operatie starten.
			 Thread.sleep(15000);

			 callbackContext.success();
		    }

		} catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }

    private Boolean isPrinterReady(Connection connection) throws ConnectionException, ZebraPrinterLanguageUnknownException {
        Boolean isOK = false;
        connection.open();
        // Creates a ZebraPrinter object to use Zebra specific functionality like getCurrentStatus()
        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
        PrinterStatus printerStatus = printer.getCurrentStatus();
        if (printerStatus.isReadyToPrint) {
            isOK = true;
        } else if (printerStatus.isPaused) {
            throw new ConnectionException("Cannot Print because the printer is paused.");
        } else if (printerStatus.isHeadOpen) {
            throw new ConnectionException("Cannot Print because the printer media door is open.");
        } else if (printerStatus.isPaperOut) {
            throw new ConnectionException("Cannot Print because the paper is out.");
        } else {
            throw new ConnectionException("Cannot Print.");
        }
        return isOK;
    }
}

