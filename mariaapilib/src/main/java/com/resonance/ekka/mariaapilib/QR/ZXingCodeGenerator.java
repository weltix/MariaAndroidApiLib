package com.resonance.ekka.mariaapilib.QR;

/**
 * Created by Святослав on 11.08.2018.
 */

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.scvngr.levelup.core.ui.view.LevelUpQrCodeGenerator;
import com.scvngr.levelup.core.util.LogManager;



import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;

/**
 * Generates LevelUp QR codes using the included ZXing library.
 */
public final class ZXingCodeGenerator implements LevelUpQrCodeGenerator {

    @Override
    @Nullable
    public LevelUpQrCodeImage generateLevelUpQrCode(String qrCodeDataString) {
        LevelUpQrCodeImage result = null;

        try {
            result = getQrCodeBitmapOrThrow(qrCodeDataString);

          /*  if (ASYNC_BACKGROUND_TASK_DELAY_ENABLED) {
                SystemClock.sleep(ASYNC_BACKGROUND_TASK_DELAY_MS);
            }*/
        } catch (WriterException e) {
            LogManager.e("Could not generate QR code", e);
        }

        return result;
    }

    /**
     * Generate a QR code from a given string (using the ZXing default encoding, ISO-8859-1).
     *
     * @param qrCodeDataString String to encode.
     * @return an immutable bitmap of the QR code that was generated.
     * @throws WriterException if there was a problem generating the bitmap
     */
    private static LevelUpQrCodeImage getQrCodeBitmapOrThrow(String qrCodeDataString)
            throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();

        /*
         * We end up encoding the QR twice, first encode the string and get the minimum size we can
         * store the result in. Once we have the proper size, we can then generate the minimal
         * bitmap that can be scaled on the code screen. This allows us to reduce the in-memory size
         * of the QR cache significantly.
         */
         final int WIDTH = 80;
         final int HEIGHT = 80;

        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        BitMatrix result =null;
    try {

        result = writer.encode(new String(qrCodeDataString.getBytes("UTF-8"), "iso-8859-1"), BarcodeFormat.QR_CODE, WIDTH, HEIGHT, hintMap);
    }catch (UnsupportedEncodingException e){
        Log.e(DBG_TAG, "UnsupportedEncodingException getQrCodeBitmapOrThrow: " + e.getMessage());
        return null;
    }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];

        // All are 0, or black, by default.
        // The output bitmap is rotated 180° from the input.
      /*  for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                if (result.get(x, y)) {
                    pixels[width * height - 1 - (offset + x)] = Color.BLACK;
                } else {
                    pixels[width * height - 1 - (offset + x)] = Color.WHITE;
                }
            }
        }
*/
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }


        // The return is [x,y].
        int[] topLeft = result.getTopLeftOnBit();

        /*
         * The target size should be a constant, but ZXing doesn't expose it anywhere, so it's
         * computed from the result. However, one can safely assume that targets in a given image
         * are all the same size and square.
         */
        int targetSize = 0;

        /*
         * The code margin should be the same on all sides, but just to be safe we use the computed
         * value when computing the target size.
         */
        int codeMargin = topLeft[0];
        int topMargin = topLeft[1];

        // Start at the top left "on" bit, then scan for the first "off" bit.
        for (int x = codeMargin; x < width; x++) {
            if (!result.get(x, topMargin)) {
                targetSize = x - codeMargin;
                break;
            }
        }

        while(((((float)(width-(2*codeMargin)))/8) - ((int)((width-(2*codeMargin))/8)))>0){

            codeMargin--;
        }


        /*
         * This returns an immutable bitmap, which is important for thread safety.
         */
      /*  LevelUpQrCodeImage codeBitmap =
                new LevelUpQrCodeImage(Bitmap.createBitmap(pixels, width, height,
                        Bitmap.Config.RGB_565), targetSize, codeMargin);*/


        Bitmap tmpBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565);
              LevelUpQrCodeImage codeBitmap =
                new LevelUpQrCodeImage(Bitmap.createBitmap(tmpBitmap, codeMargin, codeMargin, width-codeMargin*2, height-codeMargin*2), targetSize, codeMargin);

        return codeBitmap;
    }
}