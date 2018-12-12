package com.resonance.ekka.mariaapilib.IMG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.resonance.ekka.mariaapilib.Utils;

import java.io.File;

import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;

public class ClassImg {

    final int MAX_WIDTH_LOGO = 376;
    final int MAX_HEIGHT_LOGO = 192;

    public  enum RequestSizeOptions {
        RESIZE_FIT,
        RESIZE_INSIDE,
        RESIZE_EXACT
    }


    /**
     * Resize the given bitmap to the given width/height by the given option.<br>
     */
    private static Bitmap resizeBitmap(Bitmap bitmap, int reqWidth, int reqHeight, RequestSizeOptions options) {
        //масштабируем изображение
        try {
            if (reqWidth > 0 && reqHeight > 0 && (options == RequestSizeOptions.RESIZE_FIT ||
                    options == RequestSizeOptions.RESIZE_INSIDE ||
                    options == RequestSizeOptions.RESIZE_EXACT)) {

                Bitmap resized = null;
                if (options == RequestSizeOptions.RESIZE_FIT) {
                    resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false);
                } else {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    float scale = Math.max(width / (float) reqWidth, height / (float) reqHeight);
                    if (scale > 1 || options == RequestSizeOptions.RESIZE_INSIDE) {
                        resized = Bitmap.createScaledBitmap(bitmap, (int) (width / scale), (int) (height / scale), false);
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle();
                    }
                    return resized;
                }
            }
        } catch (Exception e) {
            Log.w(DBG_TAG, "Failed to resize cropped image, return bitmap before resize", e);
        }
        return bitmap;
    }





    public static Bitmap convertToBW1(Bitmap src, boolean invert){
      int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);//src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);

                // use 128 as threshold, above -> white, below -> black
                if (gray > 128) {
                    gray = 255;
                }
                else{
                    gray = 0;
                }
                gray = ((!invert)?gray:(gray-255));
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
            }
        }
        return bmOut;

     /*   int width = src.getWidth();
        int height = src.getHeight();
        Bitmap bmOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);//src.getConfig());
        //convert to grayscale
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int p = src.getPixel(x,y);

                int a = (p>>24)&0xff;
                int r = (p>>16)&0xff;
                int g = (p>>8)&0xff;
                int b = p&0xff;

                //calculate average
                int avg = (r+g+b)/3;

                //replace RGB value with avg
                p = (a<<24) | (avg<<16) | (avg<<8) | avg;

                bmOut.setPixel(x, y, p);
            }
        }
        return bmOut;*/
    }


    public byte[][] ConvertBitmapToArray(File file, RequestSizeOptions sizeOptions){

        String extension = MimeTypeMap.getFileExtensionFromUrl((Uri.fromFile(file)).toString());
        BitmapFactory.Options ImageOptions = new BitmapFactory.Options();
        ImageOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap sourceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(),ImageOptions);

        Bitmap bitmap = resizeBitmap((extension.toUpperCase().contains("PNG")?sourceBitmap:ConvertToBW(sourceBitmap)),MAX_WIDTH_LOGO, MAX_HEIGHT_LOGO,sizeOptions);//RequestSizeOptions.RESIZE_INSIDE);
        if (bitmap==null) return null;

        Bitmap bmpGrayscale = Bitmap.createBitmap( MAX_WIDTH_LOGO,  bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);

        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        Paint paint = new Paint();
        paint.setColorFilter(f);

        // поместим по центру требуемое изображение
        int LeftOffset = (MAX_WIDTH_LOGO - bitmap.getWidth())/2;
        c.drawBitmap(bitmap, LeftOffset, 0, paint);

        Bitmap BWimage = ConvertToBW(bmpGrayscale);
        bmpGrayscale.recycle();
        bitmap.recycle();

        Log.d(DBG_TAG, "H: "+BWimage.getHeight()+" W:"+BWimage.getWidth());

        byte[][] imgBuf = new byte[BWimage.getHeight()][BWimage.getWidth()/8];
        for (int y=0;y<imgBuf.length;y++)
            for (int x=0;x<imgBuf[0].length;x++)
                imgBuf[y][x] = 0;

        try {


            for (int y = 0;y < BWimage.getHeight();y++){
                String str = "";
                int shift = 0;
                int index = 0;
                for (int x = 0; x < BWimage.getWidth(); x++)
                {
                    int pix = BWimage.getPixel(x, y);

                    int R = Color.red(pix);
                    int G = Color.green(pix);
                    int B = Color.blue(pix);


                    // int gray = (int)(GS_RED * R + GS_GREEN * G + GS_BLUE * B) ;
                    int gray = (int)(0.299 * R + 0.587 * G + 0.114 * B);



                    imgBuf[y][index] |= (gray>0?(0<<shift):(1<<shift));
                    // imgBuf[y][index] |= ((pix&0x1010)>0?(1<<shift):(0<<shift));

                    shift++;
                    if (shift>=8){
                        imgBuf[y][index] = Utils.reverse(imgBuf[y][index]);
                        index++; shift=0;}
                }
                //  Log.d(DBG_TAG,  "["+y+"]"+str+"\n");
            }

        }catch (Exception e) {
            Log.e(DBG_TAG, "Exception CreateQR: " + e.getMessage());
        }


        return imgBuf;

    }


    public byte[][] ConvertBitmapToArrayOLD(File file, RequestSizeOptions sizeOptions){

        BitmapFactory.Options ImageOptions = new BitmapFactory.Options();
        ImageOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap sourceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(),ImageOptions);
        String extension = MimeTypeMap.getFileExtensionFromUrl((Uri.fromFile(file)).toString());

        Bitmap bitmap = resizeBitmap((extension.toUpperCase().contains("PNG")?sourceBitmap:ConvertToBW(sourceBitmap)),MAX_WIDTH_LOGO, MAX_HEIGHT_LOGO,sizeOptions);
        if (bitmap==null) return null;

        Bitmap bmpGrayscale = Bitmap.createBitmap( MAX_WIDTH_LOGO,  bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        float[] mat = new float[]{
                0.3f, 0.59f, 0.11f, 0, 0,
                0.3f, 0.59f, 0.11f, 0, 0,
                0.3f, 0.59f, 0.11f, 0, 0,
                0, 0, 0, 1, 0,};
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(mat/*cm*/);
        Paint paint = new Paint();
        paint.setColorFilter(f);

        // поместим по центру требуемое изображение
        int LeftOffset = (MAX_WIDTH_LOGO - bitmap.getWidth())/2;
        c.drawBitmap(bitmap, LeftOffset, 0, paint);

        Bitmap BWimage = ConvertToBW(bmpGrayscale);

         Log.d(DBG_TAG, "H: "+BWimage.getHeight()+" W:"+BWimage.getWidth());

        byte[][] imgBuf = new byte[BWimage.getHeight()][BWimage.getWidth()/8];
        for (int y=0;y<imgBuf.length;y++)
            for (int x=0;x<imgBuf[0].length;x++)
                imgBuf[y][x] = 0;

        try {


            for (int y = 0;y < BWimage.getHeight();y++){
                String str = "";
                int shift = 0;
                int index = 0;
                for (int x = 0; x < BWimage.getWidth(); x++)
                {
                    int pix = BWimage.getPixel(x, y);

                    int R = Color.red(pix);
                    int G = Color.green(pix);
                    int B = Color.blue(pix);


                   // int gray = (int)(GS_RED * R + GS_GREEN * G + GS_BLUE * B) ;
                    int gray = (int)(0.299 * R + 0.587 * G + 0.114 * B);



                    imgBuf[y][index] |= (gray>0?(0<<shift):(1<<shift));
                   // imgBuf[y][index] |= ((pix&0x1010)>0?(1<<shift):(0<<shift));

                    shift++;
                    if (shift>=8){
                        imgBuf[y][index] = Utils.reverse(imgBuf[y][index]);
                        index++; shift=0;}
                }
                //  Log.d(DBG_TAG,  "["+y+"]"+str+"\n");
            }

        }catch (Exception e) {
            Log.e(DBG_TAG, "Exception CreateQR: " + e.getMessage());
        }


        return imgBuf;

    }




    private Bitmap ConvertToBW(Bitmap bitmap){

        Bitmap bwBitmap = Bitmap.createBitmap( bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565 );
        float[] hsv = new float[ 3 ];
        for( int col = 0; col < bitmap.getWidth(); col++ ) {
            for( int row = 0; row < bitmap.getHeight(); row++ ) {
                Color.colorToHSV( bitmap.getPixel( col, row ), hsv );
                //Log.i(DBG_TAG, "hsv[" + hsv[ 2 ]+"]");
                if( hsv[ 2 ] > 0.5f ) {
                    bwBitmap.setPixel( col, row,Color.BLACK );
                } else {
                    bwBitmap.setPixel( col, row,  Color.WHITE );
                }
            }
        }
        return bwBitmap;


    }



}
