package com.resonance.ekka.mariaapilib.QR;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.resonance.ekka.mariaapilib.R;
import com.resonance.ekka.mariaapilib.Utils;
import com.scvngr.levelup.core.ui.view.LevelUpQrCodeGenerator;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;


public class ClassQR {


    public byte[][]  CreateQR(String strQR) {
        ZXingCodeGenerator QRGenerator = new ZXingCodeGenerator();
        LevelUpQrCodeGenerator.LevelUpQrCodeImage bitmap =  QRGenerator.generateLevelUpQrCode(strQR);

        Bitmap b = bitmap.getBitmap();

        byte[][] imgBuf = null;
        try {

            imgBuf =new byte[b.getHeight()][b.getWidth()/8];
            for (int y=0;y<imgBuf.length;y++)
                for (int x=0;x<imgBuf[0].length;x++)
                    imgBuf[y][x] = 0;

            for (int y = 0;y < b.getHeight();y++){
                String str = "";
                int shift = 0;
                int index = 0;
                for (int x = 0; x < b.getWidth(); x++)
                {
                    int pix = b.getPixel(x, y);
                 //   str += String.format("%s",((pix)&0x0F)==0?"#":" ");
                    imgBuf[y][index] |= ((pix&0x0F)==0?(1<<shift):(0<<shift));
                    shift++;
                    if (shift>=8){
                        imgBuf[y][index] = Utils.reverse(imgBuf[y][index]);
                        index++; shift=0;}
                }
               // Log.d(DBG_TAG,  "["+y+"]"+str+"\n");
            }

        }catch (Exception e) {
            Log.e(DBG_TAG, "Exception CreateQR: " + e.getMessage());
        }
        return imgBuf;
    }





}
