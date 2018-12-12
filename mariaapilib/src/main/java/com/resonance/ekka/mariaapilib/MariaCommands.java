package com.resonance.ekka.mariaapilib;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import com.resonance.ekka.mariaapilib.BT.BluetoothWorker;
import com.resonance.ekka.mariaapilib.IMG.ClassImg;
import com.resonance.ekka.mariaapilib.QR.ClassQR;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.resonance.ekka.mariaapilib.MariaCommands;
import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;


/*
interface MariaErrorCallback{
    void onError(String error);
}
interface MariaReadCallback{
    void onReceiveData(String buf);
}
interface MariaCommandCallback{
    void onMariaCommand(int direction, String buf );
}
*/
public class MariaCommands implements iMariaCommands {

    public interface MariaErrorCallback{
        void onError(String error);
    }
    public interface MariaReadCallback{
        void onReceiveData(String buf);
    }
    public interface MariaCommandCallback{
        void onMariaCommand(int direction, String buf );
    }

    private static MariaReadCallback callback_onRead;
    private static MariaErrorCallback callback_onError;
    private static MariaCommandCallback callback_onCommand;

    public void onCommandCallBack(MariaCommandCallback cback) {
        callback_onCommand = cback;
    }

    public void onReadCallBack(MariaReadCallback cback) {
        callback_onRead = cback;
    }

    public void onErrorCallBack(MariaErrorCallback cback) {
        callback_onError = cback;
    }

    private void do_onCommandParent(int direction, String buf ) {
        callback_onCommand.onMariaCommand(direction, buf);
        //  Log.d(DBG_TAG, "do_onReadParent: [" + buf.trim()+"]");
    }


    private void do_onReadParent(String buf) {
        callback_onRead.onReceiveData(buf);
      //  Log.d(DBG_TAG, "do_onReadParent: [" + buf.trim()+"]");
    }

    private void do_onErrorParent(String Error) {
        callback_onError.onError(Error);
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    public @interface MethodInfo {
        String Params()[];
        int idMetod();
    }

    private final String DBG_TAG = "DBG_TAG";
   



    private String errors = "";
    private MariaErrorHandler errorHandler;

   // private Context mContext;
    private MariaAPI mariaAPI = null;
    private static ClassCheck Check = null;
    private static Context mContext;
    public MariaCommands(MariaAPI api, Context context)
    {
        mContext = context;
        mariaAPI=api;
        errorHandler = new MariaErrorHandler();
        Check = new ClassCheck();
    }

    /********************************** Service methods **********************************/
    private String sendCommandInner(String command) throws MariaException {
        return sendCommand(command, true);
    }

    private String sendCommand(String command) throws MariaException {
        return sendCommand(command, false);
    }

    private String sendCommand(String command, boolean inner) throws MariaException{
        Log.d(DBG_TAG, "sendCommand : " + command);
        String res = "";
        try {
            StringBuilder result = new StringBuilder();

            while (mariaAPI.GetAvailable() > 0)
            {
                byte[] trashArr = mariaAPI.read(100);
            }

            do_onCommandParent(1,command);

            byte[] packet = preparePacket(command);
          //  do_onCommandParent(1,new String(packet));
            mariaAPI.write(packet);
            // Log.d(DBG_TAG, "++++write(packet): " + new String(packet));
            String tmpStr="";
            byte[] TmpArr;
            boolean HeadFound = false;
            do {
                TmpArr = mariaAPI.read(2000);//!!! timeout is not less than
                if (TmpArr==null){ break;}
                // Log.d(DBG_TAG, "TmpArr.length: "+TmpArr.length);

                for (int i=0;i<TmpArr.length;i++)
                {
                    if (TmpArr[i]==(byte)253)
                    {
                        HeadFound = true;
                        if (result.length()>0)
                            result.delete(0,result.length());
                        continue;
                    }
                    // total.put(cntBytes, TmpArr[i]);
                    //  cntBytes ++;
                    if (TmpArr[i]!=(byte)254) {
                       // if (result.lastIndexOf("=XMLFILE=") == -1)
                            result.append(Encoder866.from866Bytes(new byte[]{TmpArr[i]}));


                       // else
                         //   result.append(new String(new byte[]{TmpArr[i]},"windows-1251"));
                        // Log.d(DBG_TAG, ">>result: " + result);
                    }
                    if ((TmpArr[i]==(byte)254)&&(HeadFound))
                    {
                        HeadFound = false;
                        tmpStr = result.toString().substring(0, result.length() - 1);

                        do_onCommandParent(2,tmpStr);//команды для отладки

                        // Log.d(DBG_TAG, ">>tmpStr: [" + tmpStr+"]");

                        if(!tmpStr.contains(ANSW_READY) && !tmpStr.contains("WAIT")
                                && !tmpStr.contains("DONE") && !tmpStr.contains("WRK") && !tmpStr.contains("PRN")) {
                            checkForErrors(tmpStr);
                            res += tmpStr.trim();
                        }
                        if(tmpStr.contains("WAIT") || tmpStr.contains("WRK") || tmpStr.contains("PRN")) {
                            do_onReadParent(tmpStr);
                        }

                        if ((res.trim().length()==0)&&(tmpStr.contains(ANSW_READY)))
                            res += tmpStr.trim();

                    }
                }
            } while (!tmpStr.trim().equals(ANSW_READY));

            Log.d(DBG_TAG, "sendCommand result:[" + res+"]");
            if (!inner)//передавать данные родителю
              do_onReadParent(res);

           /* if(errors.length() > 0)
            {
                //  Log.d(DBG_TAG, "errors.length(): " + errors.length());
                throw new MariaException(errors);
            }*/

       /* } catch (IOException e) {
            Log.d(DBG_TAG, "sendCommand IOException: " + e.getMessage());
            e.printStackTrace();
            throw new MariaException(e.getClass().toString() + ": " + e.getMessage());*/
        } catch (Exception e){
            Log.e(DBG_TAG, "sendCommand Exception: [" + e.getMessage()+"]");
            e.printStackTrace();
            if (!inner)//передавать данные родителю
                do_onErrorParent(e.getMessage().trim());
            errors = "";
            throw new MariaException(e.getClass().toString() + ": " + e.getMessage());
        }
        return res;
    }




    private void checkForErrors(String value){
        //  Log.d(DBG_TAG, "checkForErrors: "+value);
        if(errorHandler.hasError(value))
        {
            do_onErrorParent(value.trim());
            errors += value.trim() + " " + errorHandler.getError(value).trim()+"\n" ;
        }
    }

    private byte[] preparePacket(String StrData) {

        byte[] dataBytes =Encoder866.get866Bytes(StrData);

        byte[] bytes = new byte[dataBytes.length + 3];

       System.arraycopy(dataBytes, 0, bytes, 1, dataBytes.length);

            bytes[0] = (byte) 253;
            bytes[bytes.length - 1] = (byte) 254;
            bytes[dataBytes.length + 1] = (byte) (dataBytes.length + 1);

            int crc = crc16(bytes);

            byte[] preparedBytes = new byte[bytes.length + 2];

            System.arraycopy(bytes, 0, preparedBytes, 0, bytes.length);
            preparedBytes[preparedBytes.length - 2] = (byte) (crc & 0xff);
            preparedBytes[preparedBytes.length - 1] = (byte) ((crc >> 8) & 0xff);
            return preparedBytes;
            //return bytes;


    }

    private int crc16(byte[] bytes) {
        int ofs = 0;
        int crc16 = 0;
        while (ofs < bytes.length) {
            crc16 ^= 0xff & bytes[ofs];

            int a = (crc16 ^ (crc16 << 4)) & 0x00FF;
            crc16 = (crc16 >> 8) ^ (a << 8) ^ (a << 3) ^ (a >> 4);
            ofs += 1;
        }
        return crc16;
    }



    private String getHexArticleNumber(int articleNumber){
        Integer remainder = articleNumber % 1000;
        int index = (articleNumber - remainder)/1000;

        char[] hex = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        String result = hex[index] + String.format("%03d", remainder);
        return result;
    }

    private int getDecArticleNumber(String articleNumber){
        String greater   = articleNumber.substring(0, 1);
        String remainder = articleNumber.substring(1);

        String hex = "0123456789ABCDEF";

        int great = hex.indexOf(greater);

        return great * 1000 + Integer.valueOf(remainder);
    }



    private boolean connect(int CountTryToConnect)
    {
       // Log.d(DBG_TAG, "connect...");
        boolean res = false;
        try {

            int CountConnections = CountTryToConnect;
            while ((CountConnections--) > 0) {

                if (!mariaAPI.write(new byte[]{0x55})) {
                    Log.e(DBG_TAG, "connect write error ");
                    break;
                }
                Utils.sleep(200);
                if (!mariaAPI.write(new byte[]{0x55})) {
                    Log.e(DBG_TAG, "connect write error ");
                    break;
                }
                Utils.sleep(500);

                StringBuilder result = new StringBuilder();
                Log.d(DBG_TAG, "connect " + CountConnections);
                do {

                        byte[] mmBuffer = mariaAPI.read(100);
                        if (mmBuffer == null) break;

                       // Log.d(DBG_TAG, "result: " + new String(mmBuffer, ENCODING_CHARSET));

                        result.append(new String(mmBuffer, ENCODING_CHARSET));

                        if (result.indexOf(ANSW_READY) >= 0) {
                            Log.d(DBG_TAG, "Connection established");
                            res = true;
                        }
                } while (!res);
                if (res) break;
                else{
                     mariaAPI.ResetConnection(10000);
                }

            }
        }catch (Exception e){
            Log.e(DBG_TAG, "connect Exception  "+e);
        }
        return res;
    }


   /* private void TakeTaxesValues() {
        try {
            List<Map<String, String>> taxList = GetTaxesInfo(false);

            for (int j = 0; j < taxList.size(); j++) {
                char taxname = TaxName[j];
                int typetax = Integer.parseInt(taxList.get(j).get("taxType"));
                String taxRate = taxList.get(j).get("taxValue");
                taxes[j] = new Tax(taxname, typetax, taxRate);
                Log.w(DBG_TAG, "[" + j + "] key taxNum: " + taxList.get(j).get("taxNum") + " value:" + taxList.get(j).get("taxValue") + " taxType:" + taxList.get(j).get("taxType"));
            }
        }catch (Exception e){};
    }*/


    /***************************************************************************************************************
                                                Установка соединения
     ***************************************************************************************************************/

    /*
     * Установка соединения с ЭККР
     * <br>
     * <br>
     * @param  <CashierName>           до 9 символов идентификатор кассира
     * @param  <CashierName>           10 символов текущий пароль
     *
     * @return                        0/1 успех/ошибка
     */
    @MethodInfo(idMetod = 1, Params = {"CashierName","EkkaPassword"} )
    public int Init(String CashierName, String EkkaPassword) throws MariaException{

        if (CashierName.length()==10)return -1;

        int result = 1;
        Log.d(DBG_TAG, "Init...");

        if (checkConnection("#init#").indexOf("#init#")>=0) {//добавил по наставлению 4.12.18
            return 0;
        }
        else
        {

            if (!connect(5)) return 1;
            try {

                result = (sendCommand(CMD_UPAS + EkkaPassword + CashierName).trim().contains(ANSW_READY) ? 0 : 1);

            } catch (MariaException e) {
                if (e.getMessage().startsWith("SOFTUPAS")) {
                    throw new MariaException("SOFTUPAS Неверный пароль или идентификатор кассира");
                }
                throw e;
            }
            if (result == 0) {
                try {
                    HashMap<String, String> fi = GetFiscalInfo();
                    int totalSales = Integer.parseInt(fi.get("totalSalesTurnover"));
                    int totalReturn = Integer.parseInt(fi.get("totalRefundTurnover"));
                    if ((totalSales + totalReturn) == 0)//если смена не открыта - настроим артикульную таблицу
                        result = SetArticleMode(2);
                    // Log.d(DBG_TAG, "Init totalSales:" +totalSales+ " totalReturn:"+totalReturn);
                } catch (Exception e) {
                    Log.e(DBG_TAG, "Init Exception:" + e.getMessage());
                    result = 1;
                }
                ;
            }
        }
        return  result;
    }

    /***************************************************************************
    *                           Служебные документы
    ****************************************************************************/
    /**
     * Открытие служебного документа.
     * <br>
     * <br>
     * На чековой ленте печатается строка 'СЛУЖБОВИЙ ДОКУМЕНТ'.
     *
     * @return                        0/1 успех/ошибка
     *
     *
     */
    @MethodInfo(idMetod = 2, Params = "")
    public int OpenTextDocument() throws MariaException{
        String tmpRes = sendCommand(CMD_DBEG);

        return (tmpRes.trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Закрытие служебного документа.
     * <br>
     * <br>
     *
     * @return                        0/1 успех/ошибка
     *
     */
    @MethodInfo(idMetod = 3, Params = "")
    public int CloseTextDocument() throws MariaException{
        return (sendCommand(CMD_PRTX).trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Открытие квитанции платежного терминала.
     * <br>
     * <br>
     * Начинает печать документа «Квитанцiя платiжного термiнала».
     * Строки для печати загружаются вызовом FreeTextLine, завершение печати и закрытие
     * документа производится командой CloseTextDocument.
     *
     * @return                        int 0  в случае успеха,
     *                                пробрасывает исключение MariaException в случае неудачи.
     */
    @MethodInfo(idMetod = 4, Params = "")
    public int OpenPaymentTerminalReceipt() throws MariaException{
        return (sendCommand(CMD_SLPB).trim().contains(ANSW_READY)?0:1);
    }

    /***************************************************************************
     *                           Формирование чеков
     ****************************************************************************/
    /**
     * Прогон чековой ленты.
     *
     * @param  steps                  int до 4-х символов число от 0 до 65535 –
     *                                количество шагов двигателя протяжки чековой ленты (шаг 0,125 мм).
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 7, Params = {"steps" })
    public int Feed(int steps) throws MariaException{
        if (!isConvertableToNumeric(steps)) return 2;

        return(sendCommand(CMD_FEED + steps).contains(ANSW_READY)?0:1);
    }

    /**
     * Отмена чека.
     * <br>
     * <br>
     * Используйте эту команду для отмены операции регистрации продажи после
     * вызова openCheck и до вызова closeCheck или для гарантированной отмены не
     * закрытого чека после аварийных ситуаций в канале связи или в ПО ВУ.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 5, Params = "")
    public int AbortCheck() throws MariaException{
        String tmpRes = sendCommand(CMD_CANC);
        Check.CloseCheck();
        return (tmpRes.trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Создание копии чека.
     * <br>
     * <br>
     * После успешного создания оригинала фискального чека доступна команда
     * создания копии. Допустимо отпечатать только одну копию
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 8, Params = "")
    public int CheckCopy() throws MariaException{
        String tmpRes = sendCommand(CMD_COPY);
        return (tmpRes.trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Определение номера возвратного чека.
     * <br>
     * <br>
     * Имеет значение для наборов сообщений '0','1' (setServiceMessagesKit). Переданные в
     * команде номера будут отпечатаны за словами "ПОВЕРНЕННЯ ПО ЧЕКУ N" и на
     * следующих двух строках (в зависимости от длины numbersList). Используйте команду
     * перед открытием чека возврата, т.е. до команды openCheck.
     *
     * @param  numbersList            String 1 от 1-го до 86-ти символов список номеров чеков.
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 12, Params = {"numbersList"})
    public int SetReturnCheckNumber(String numbersList) throws MariaException{
        sendCommand(CMD_BCHN + numbersList);
        return 0;
    }

    /**
     * Производится печать «нулевого чека»
     *<br>
     *<br>
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
    */
    @MethodInfo(idMetod = 14, Params = "")
    public int NullCheck() throws MariaException{
        return (sendCommand(CMD_NULL).trim().contains(ANSW_READY)?0:1);
    }
    /**
     * Открытие чека.
     *<br>
     *<br>
     * Для упрощения алгоритмов функционирования ПО ВУ перед открытием нового
     * чека рекомендуется вызов функции cancelCheck для гарантированной отмены не
     * закрытого документа или не закрытого чека после аварийных ситуаций в канале
     * связи или в ПО ВУ – отпадает необходимость контролировать состояние чека
     * (открыт/закрыт) по «признаку ожидаемой команды создания документов».

     * @param  departmentName         String до 35-ти символов идентификатор (наименование) торгового отдела.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 15, Params = {"departmentName"})
    public int OpenCheck(String departmentName) throws MariaException{


        String tmpRes = sendCommand(CMD_PREP + departmentName);
        if (tmpRes.trim().contains(ANSW_READY)) {
            Check.OpenCheck(false);
            return 0;
        }
        return 1;
    }

    /**
     * Закрытие чека.
     * <br>
     * <br>
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     */
    @MethodInfo(idMetod = 86, Params = {""})
    public  int CloseCheck()throws MariaException
    {
        if (!Check.isOpenCheck())return 1;
        String strData = Check.getStringCOMP();
        return  (sendCommand(CMD_COMP+strData).contains(CMD_COMP)?0:1);
    }

    /**
     * Служебная информация в чеке.
     * <br>
     * <br>
     * Каждый следующий вызов с одинаковым значением параметра position
     * программирует следующую по счету сверху вниз строку для печати.
     * Информация, заданная вызовом setServiceInfo, сохраняется до того момента,
     * пока она не будет отпечатана либо пока не будет выполнена команда cancelServiceInfo.
     * Для чека командами setServiceInfo с параметром position == 0 можно подготовить не
     * более 100 строк, а с параметром position == 1 не более 300.
     * Ограничения при применении команды в составе последовательности, формирующей чек.
     * 1. При использовании фискального модуля в составе ЭККА будут отпечатаны
     * первые 21 символов строки text при значении fontStyle равном 1.
     * 2. При использовании фискального модуля в составе РРКО будут отпечатаны
     * первые 30 (при fontStyle==0) или 15 (при fontStyle==1) символов строки text.
     * Список строк, созданных с position==0, печатается после:
     * - получения первой команды inputCheckFiscalDataRegisterNew(или inputCheckFiscalDataUseProgrammed).
     * При этом список этих строк очищается и доступен для загрузки новыми строками для
     * возвратной фискальной части (inputCheckFiscalDataRegisterNewRefund(inputCheckFiscalDataUseProgrammedRefund)).
     * - получения первой команды inputCheckFiscalDataRegisterNewRefund(или inputCheckFiscalDataUseProgrammedRefund).
     * Если им предшествовали команды inputCheckFiscalDataRegisterNew(или inputCheckFiscalDataUseProgrammed), то
     * после печати непустого
     * списка, созданных с position==1 для реализационной части.
     * Список строк, созданных с position==1, печатается после:
     * - первого вызова inputCheckFiscalDataRegisterNewRefund(или inputCheckFiscalDataUseProgrammedRefund),
     * если им предшествовали вызовы inputCheckFiscalDataRegisterNew(или inputCheckFiscalDataUseProgrammed).
     * При этом список этих строк очищается и доступен для загрузки новыми строками для возвратной
     * фискальной части.
     * - получения команды closeCheck.
     * Таким образом может быть создано обрамление из служебной текстовой информации
     * отдельно как для реализационной, так и для возвратной частей чека.
     * Наличие подготовленных строк текстовой информации, размещаемых после
     * фискальной информации вызывает автоматическую печать сообщения «СЛУЖБОВА IНФОРМАЦIЯ».
     *
     * @param  position               int - 1 символ 0/1 признак расположения текста до/после фискальной информации.
     *
     * @param  fontStyle              int - 1 символ [0..3] признак печати строки модифицированным шрифтом:
     *                                “0” – нет модификации,
     *                                “1” - c удвоенной шириной,
     *                                “2” – с удвоенной высотой,
     *                                “3” - c удвоенными шириной и высотой.
     *
     * @param  text                   String - до 43-х символов собственно строка для печати.
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 9, Params = {"position","fontStyle","text" })
    public int FreeTextLine(int position, int fontStyle, String text) throws MariaException{
        if ((!isConvertableToNumeric(position))||
            (!isConvertableToNumeric(fontStyle))) return 2;
        String randomSymbol = "1";
        sendCommand(CMD_TEXT + position + randomSymbol + fontStyle + text.trim());

        return 0;
    }

    /**
     * Отмена служебной информации в чеке.
     * <br>
     * <br>
     * Очищает массивы и счетчики строк, подготовленных вызовами setServiceInfo.
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 10, Params = "")
    public int ClearFreeTextLines() throws MariaException{
        sendCommand(CMD_CTXT);
        return 0;
    }

    /**
     * Дополнительная информация о товаре (услуге).
     * <br>
     * <br>
     * Переданный в качестве info текст будет отпечатан во время исполнения
     * следующей команды inputCheckFiscalDataRegisterNew (inputCheckFiscalDataRegisterNewRefund) или inputCheckFiscalDataUseProgrammed
     * (inputCheckFiscalDataUseProgrammedRefund). Для каждой такой
     * команды с помощью setProductAdditionalInfo можно подготовить только один блок из от 1 до 2-х
     * строк расширенной информации.
     *
     * @param  info                   String  до 86-ти символов текстовой информации.
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 6, Params = {"info"})
    public int LongName(String info) throws MariaException{
        return (sendCommand(CMD_FINF + info).contains(ANSW_READY)?0:1);

    }
    /**
     * Открытие новой группы фискальных позиций в пределах чека.
     *<br>
     *<br>
     * Применяется при не открытой группе в любой момент после открытия чека
     * вызовом функции openCheck, независимо от количества уже созданных фискальных позиций в чеке.
     * На чеке будет отпечатано наименование группы позиций, указанное в groupName.
     * После вызова openNewGroupOfFiscalPositionsWithinCheck необходимое количество раз применяются функции
     * ввода фискальных данных чека – создание фискальных позиций чека, принадлежащих одной группе.
     * Закрытие группы с печатью промежуточного итога по группе производится
     * вызовом функции closeGroupOfFiscalPositionsWithinCheck.

     * @param  groupName              String от 1 до 21-го символа наименование новой группы позиций.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 35, Params = {"groupName"})
    public int OpenGoodsGroup(String groupName) throws MariaException{
        return (sendCommand(CMD_GRBG + groupName).trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Закрытие группы фискальных позиций в пределах чека.
     *<br>
     *<br>
     * Применяется при открытой вызовом функции openNewGroupOfFiscalPositionsWithinCheck группе
     * в любой момент после создания необходимого количества фискальных позиций.
     * На чеке будет отпечатана итоговая информация по закрываемой группе:
     * - итог по скидкам,
     * - итог по надбавкам,
     * - сумма операций с учетом скидок-надбавок.

     * @param  discountsTotalsName    String
     *                                22 символа - наименование итога по скидкам в пределах закрываемой группы.
     * @param  surchargesTotalsName   String
     *                                22 символа - наименование итога по надбавкам в пределах закрываемой группы.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 36, Params = {"discountsTotalsName","surchargesTotalsName"})
    public int CloseGoodsGroup (String discountsTotalsName, String surchargesTotalsName) throws MariaException{
        byte[] additionalArr=null;
        String params = discountsTotalsName.trim() + surchargesTotalsName.trim();
        if (params.trim().length()<22){
            additionalArr = new byte[22-params.trim().length()];
            Arrays.fill(additionalArr, (byte)' ');
        }
        String tmpRes =  sendCommand(CMD_GREN + params+((additionalArr!=null)?new String(additionalArr):""));
        return (tmpRes.trim().contains(ANSW_READY))?0:1;
    }


    /**
     * Ввод фискальных данных чека
     *<br>
     *<br>
     * Применяется при открытой вызовом функции openNewGroupOfFiscalPositionsWithinCheck группе
     * в любой момент после создания необходимого количества фискальных позиций.
     * На чеке будет отпечатана итоговая информация по закрываемой группе:
     * - итог по скидкам,
     * - итог по надбавкам,
     * - сумма операций с учетом скидок-надбавок.

     * @param  nameTov                до 24 символов наименование товара
     * @param  count                  количество единиц товара
     * @param  сost                   стоимость единицы товара (услуги)
     * @param isDivisible             признак делимости, где'0' - делимый, при этом значение <count> интерпретируется как xxx,xxx.
     *                                '1' - неделимый, при этом значение <count> интерпретируется как xxxxxx.
     * @param tax1                    1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param tax2                    2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param goodsNumber             9-ти символьный номер артикула
     *
     *
     * @return                        0/1/2/3  успех/ошибка протокола/чек не открыт/неправильные параметры
     *                                MariaException в случае неудачи
     */

    @MethodInfo(idMetod = 85, Params = {"nameTov", "count", "Cost", "isDivisible", "tax1", "tax2", "goodsNumber" })
    public int FiscalLine(String nameTov, double count, double сost, int isDivisible, int tax1, int tax2, int goodsNumber) throws MariaException{
        if ((!isConvertableToNumeric(count))||
                (!isConvertableToNumeric(сost))||
                (!isConvertableToNumeric(isDivisible))||
                (!isConvertableToNumeric(tax1))||
                (!isConvertableToNumeric(tax2))||
                (!isConvertableToNumeric(goodsNumber))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 3;
        }

        if (!Check.isOpenCheck())return 2;

        if (isDivisible==0)//делимый товар
        {
            if(((int)count)>999)return 3;
        }

        try {
            String strData = Check.CreateFiscLine(nameTov, count, сost, isDivisible, tax1, tax2, goodsNumber,0,"",0);
            String res = sendCommand(((!Check.isReturnOperation())?CMD_FICD :CMD_BFCD)+ strData);
            return (res.contains(CMD_FISC)?0:1);
        }catch(Exception e){
            Log.e(DBG_TAG, "FiscalLine Exception: " + e.getMessage());
        }

        return 0;

    }

    /**
     * Ввод фискальных данных чека
     *<br>
     *<br>
     * Применяется при открытой вызовом функции openNewGroupOfFiscalPositionsWithinCheck группе
     * в любой момент после создания необходимого количества фискальных позиций.
     * На чеке будет отпечатана итоговая информация по закрываемой группе:
     * - итог по скидкам,
     * - итог по надбавкам,
     * - сумма операций с учетом скидок-надбавок.

     * @param nameTov                  до 24 символов наименование товара
     * @param count                    количество единиц товара
     * @param cost                     стоимость единицы товара (услуги)
     * @param isDivisible              признак делимости, где'0' - делимый, при этом значение <count> интерпретируется как xxx,xxx.
     *                                '1' - неделимый, при этом значение <count> интерпретируется как xxxxxx.
     * @param tax1                     1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param tax2                     2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param goodsNumber              9-ти символьный номер артикула
     * @param isDiscountOrExtraCharge  0/1/2 – нет скидки(надбавки)/скидка/надбавка
     * @param nameDiscount             название скидки
     * @param discountOrExtraChargeSum cумма скидки
     *
     * @return                        0/1/2/3  успех/ошибка протокола/чек не открыт/неправильные параметры
     *
     */

    @MethodInfo(idMetod = 90, Params = {"nameTov", "count", "singleCost", "isDivisible", "tax1", "tax2", "goodsNumber", "isDiscountOrExtraCharge", "nameDiscount" , "discountOrExtraChargeSum" })
    public int FiscalLine(String nameTov, double count, double cost, int isDivisible, int tax1, int tax2, int goodsNumber, int isDiscountOrExtraCharge, String nameDiscount, double discountOrExtraChargeSum) throws MariaException{
        if ((!isConvertableToNumeric(count))||
                (!isConvertableToNumeric(cost))||
                (!isConvertableToNumeric(isDivisible))||
                (!isConvertableToNumeric(tax1))||
                (!isConvertableToNumeric(tax2))||
                (!isConvertableToNumeric(goodsNumber))||
                (!isConvertableToNumeric(isDiscountOrExtraCharge))||
                (!isConvertableToNumeric(discountOrExtraChargeSum))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 4;
        }
        if (!Check.isOpenCheck())return 2;

        if (isDivisible==0)//делимый товар
        {
            if(((int)count)>999)return 3;
        }

        if (!Check.isOpenCheck())return 1;

        if ((isDiscountOrExtraCharge>2)||((isDiscountOrExtraCharge<0))) return 5;

        try {
            String strData = Check.CreateFiscLine(nameTov, count, cost, isDivisible, tax1, tax2, goodsNumber, isDiscountOrExtraCharge, nameDiscount, discountOrExtraChargeSum);
            String res = sendCommand(((!Check.isReturnOperation())?CMD_FICD :CMD_BFCD)+ strData);
            return (res.contains(CMD_FISC)?0:1);
        }catch(Exception e){
            Log.e(DBG_TAG, "FiscalLine 2 Exception: " + e.getMessage());
        }
        return 1;
    }

    /**
     * Добавление оплаты в фискальный чек
     * <br>
     * <br>
     * @param cash                  сумма наличными
     * @param noncash3              сумма безналичных 3
     * @param noncash2              сумма безналичных 2
     * @param noncash1              сумма безналичных 1
     *
     * @param NameTransaction       описание
     *
     * @return                      возвращает 0 в случае успеха, пробрасывает исключение
     *                              MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 87, Params = {"cash","noncash3","noncash2","noncash1","idTransaction"})
    public  int AddPayment(double cash, double noncash3, double noncash2, double noncash1, String NameTransaction){
        if ((!isConvertableToNumeric(cash))||
                (!isConvertableToNumeric(noncash3))||
                (!isConvertableToNumeric(noncash2))||
                (!isConvertableToNumeric(noncash1))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 2;
        }

        if (!Check.isOpenCheck())return 1;
        return Check.AddPayment( cash,  noncash3,  noncash2,  noncash1,  NameTransaction);

    }

    /**
     * Добавление оплаты в фискальный чек
     * <br>
     * <br>
     * @param sum                  сумма
     * @param paymentType          0/1/2/3   наличные/безналичные 1/безналичные2/безналичные3

     *
     * @return                        возвращает 0 в случае успеха
     *
     */
    @MethodInfo(idMetod = 88, Params = {"sum","paymentType"})
    public int AddPayment ( double sum , int paymentType ) {
        if (!Check.isOpenCheck())return 1;
        if (!isConvertableToNumeric(sum))return 1;

        return Check.AddPayment(sum, paymentType);
    }

     /**
     * Открывает фискальный чек возврата
     * <br>
     * <br>
     * @param departmentName             название торгового отдела
     * @param NumberCheckReturn          № чека, по которому производится возврат
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 89, Params = {"departmentName","NumberCheckReturn"})
    public int OpenReturnCheck(String departmentName, String NumberCheckReturn) throws MariaException {

        sendCommandInner(CMD_CANC);
        if (!sendCommand(CMD_TMOD + "0").contains(ANSW_READY))return 1;
        if (!sendCommand(CMD_BCHN + NumberCheckReturn).contains(ANSW_READY))return 1;
         if (sendCommand(CMD_PREP + departmentName).contains(ANSW_READY)) {
             Check.OpenCheck(true);
             return 0;
         }else
             return 1;
    }

    /*
     * Переключает текущий чек в режим возврат  товара
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 91, Params = {""})
    public  int SetCurrentCheckForReturn ( ){
        if (!Check.isOpenCheck())return 1;
        Check.SetReturnOperation();
        return 0;
    }

    /**
     * Указание типа выплаты.
     * Для указания типа выплаты фискальной позиции секции возврата.
     * <br>
     * <br>
     * По умолчанию для каждой фискальной позиции секции возврата
     * установлен тип выплаты «0». При необходимости изменить тип выплаты
     * непосредственно перед FiscalLine
     *
     * @param  paymentID              int 1 символ [0..3] идентификатор типа выплаты:
     *                                0 – возврат товара
     *                                1 – рекомпенсация услуги
     *                                2 – прием ценностей под залог
     *                                3 – выплата выигрыша
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 76, Params = {"paymentID"})
    public int SetPaymentType(int paymentID) throws MariaException{
        sendCommand("cnmo" + paymentID);
        return 0;
    }


    /***************************************************************************
     *                           Служебные Операции
     ****************************************************************************/

    /**
     * Служебное внесение-изъятие наличных средств.
     * <br>
     * <br>
     * Соответствующим образом изменяются внутренние регистры с информацией о
     * движении средств – увеличиваются суммы внесения-изъятия и корректируется исходящий остаток.
     * Проверяется корректность (достаточность) исходящих остатков для операций изъятия.
     * В случае нехватки средств операция отменяется с сообщением об
     * ошибке ‘SOFTNEED’.
     * В случае переполнения регистров учета по операции внесения - операция
     * отменяется с сообщением об ошибке ‘SOFTOVER’.
     *
     * @param  movedirection          0/1 - изъятие/внесение
     * @param  cashsum                int - 10-ти разрядная сумма внесения-изъятия
     * @param  description            String (необязательный параметр)- до 120 символов текстовое описание операции
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 13, Params = {"movedirection", "cashsum", "description"})
    public int MoveCash(int movedirection, double cashsum, String description) throws MariaException{
        if ((!isConvertableToNumeric(movedirection))||
                (!isConvertableToNumeric(cashsum))){
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 4;
        }

        DecimalFormat twoD = new DecimalFormat("#.##");
        int sum = (int)(Double.valueOf(twoD.format(cashsum).replace(",","."))*100);

        //Log.w(DBG_TAG, "MoveCash:" + CMD_CAIO + ((movedirection == 0) ? "O" : "I") + String.format("%010d", sum) + description);
        return  (sendCommand(CMD_CAIO + ((movedirection == 0) ? "O" : "I") + String.format("%010d", sum) + description).contains(ANSW_READY)?0:1);
    }

    /**
     *  Открытие новой смены без регистрации оборотов.
     *  Увеличивает номер Z-отчета на 1 и сбрасывает признак выполненного
     *  Z-отчета.
     *  Команда доступна к выполнению только на зарегистрированном ФМ в состоянии
     *  «Z-отчет выполнен». После применения команды возможна регистрация оборотов.
     *  Применяется с целью записи в ФП и печати нового Z-отчета с нулевыми
     *  суммами по вызову createZReport.
     *
     * @return                         возвращает 0 в случае успеха, пробрасывает исключение
     *
     */
    @MethodInfo(idMetod = 16, Params = "")
    public int OpenBusinessDay() throws MariaException{
      return (sendCommand(CMD_nrep).trim().contains(CMD_nrep)?0:1);
    }

    /**
     *  Закрывает фискальную смену и печатает Z-отчёт
     *
     * @return              возвращает 0 в случае успеха, пробрасывает исключение
     *
     */

    @MethodInfo(idMetod = 17, Params = {""})
    private int CloseBusinessDay() throws MariaException{
        Map<String,String> ConfigEkka = GetPrinterConfig ();
        int isReported = Integer.parseInt(ConfigEkka.get("isReported"));
        int result  = 0;
        if (isReported==0)
            result = ZReport();
        return result;
    }

    /***************************************************************************
     *                           Графика
     ***************************************************************************/

    /**
     * Штрихкодовые символы «Код128» или «EAN-13» в чеке.
     * <br>
     * <br>
     * После печати графического образа кода будет автоматически отпечатано
     * символьное содержимое кодированной информации в виде последовательности
     * пар символов. Знаки с номерами 101 и 103 отображаются в виде «>A». Знаки
     * с номерами 100 и 104 отображаются в виде «>B». Знак с номером 102 отоб-
     * ражаются в виде «>F». Знак с номером 105 отображаются в виде «>C». Кон-
     * трольный и знак «STOP» не отображаются. Остальные информационные знаки с
     * номерами от 0 до 99 отображаются соответственно парами символов от «00»
     * до «99». Применение команды создает строку с графической информацией плюс от 1-й
     * до 2-х строк текстовой информации (содержимое кода) в соответствии с
     * требованиями и ограничениями, аналогичными команде ‘TEXT’.
     *
     * @param  position               int - 1 символ 0/1 признак расположения текста до/после фискальной информации.
     *
     * @param  codeType               String - 1 символ указатель на вид кода. Символ ‘E’(69dec) – печать EAN13,
     *                                любые другие символы – печать «Код128» по ДСТУ 3776-98.
     *
     * @param  code                   String - от 4-х до 17-ти символов – номера знаков Код128. Первый символ
     *                                обязательно должен находиться в диапазоне [103dec..105dec] – один из
     *                                3-х возможных стартовых знаков в соответствии с ДСТУ 3776-98. Остальные символы - с кодами из диапазона
     *                                [0dec..102dec]. Контрольный знак и знак «STOP» будут добавлены автоматически.
     *                                Таким образом, строка символов code начинается с требуемого знака
     *                                «START» (CODEA, CODEB или CODEC), вслед за которым может следовать от 3-х
     *                                до 16-ти информационных знаков кода (без контрольного знака и без знака «STOP»).
     *                                После печати графического образа кода будет автоматически отпечатано
     *                                символьное содержимое кодированной информации в виде последовательности
     *                                пар символов. Знаки с номерами 101 и 103 отображаются в виде «>A». Знаки
     *                                с номерами 100 и 104 отображаются в виде «>B». Знак с номером 102 отоб-
     *                                ражаются в виде «>F». Знак с номером 105 отображаются в виде «>C». Контрольный и
     *                                знак «STOP» не отображаются. Остальные информационные знаки с номерами от 0
     *                                до 99 отображаются соответственно парами символов от «00» до «99».
     *                                Применение команды создает строку с графической информацией плюс от 1-й
     *                                до 2-х строк текстовой информации (содержимое кода) в соответствии с
     *                                требованиями и ограничениями, аналогичными функции setServiceInfo.
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 19, Params = {"position","codeType","code" })
    public int PrintBarcode(int position, String codeType, String code) throws MariaException{
        if (!isConvertableToNumeric(position)) return 2;
        String randomSymbol = "1";
       String tmpRes  = sendCommand(CMD_PCOD + position + randomSymbol + codeType + code);

        return (tmpRes.trim().contains(ANSW_READY))?0:1;
    }


    /**
     * Печатает QR-код на чеке
     * <br>
     * <br>
     * @param StringForQR       Строка данных
     * @param scale             размер от 1 до 10
     * @return                  возвращает 0 в случае успеха, пробрасывает исключение
    */
    @MethodInfo(idMetod = 92, Params = {"StringForQR","scale"})
    private  int PrintQR_OLD (String StringForQR, int scale ) throws MariaException {

        if (!isConvertableToNumeric(scale)) return 2;
        int res = 0;
        final int MAX_SIZE_QR = 49;
        int tmpScale = scale;



        try {
            ClassQR qr = new ClassQR();
            byte[][] qrBuf = qr.CreateQR(StringForQR);
            if (qrBuf == null) return 1;

            int CountRows = qrBuf.length;
            int CountBytesHorizontal = qrBuf[0].length;

            while((tmpScale*CountBytesHorizontal)>=MAX_SIZE_QR) {
                tmpScale --;
            }

            String tmpRes = sendCommand("GCSZ"+String.format("%02d%02d%03d%03d",CountBytesHorizontal,tmpScale, CountRows,tmpScale));
            if(!tmpRes.trim().contains(ANSW_READY))return 1;


            for (int rows = 0;rows<CountRows;rows++){
                String str = "";
                for (int hor = 0;hor<CountBytesHorizontal;hor++) {
                    str += String.format("%02X", qrBuf[rows][hor]);
                }
                tmpRes=sendCommand(CMD_GCLN + str);
                if(!tmpRes.trim().contains(ANSW_READY))return 1;
            }
        }catch (Exception e){
            Log.e(DBG_TAG, "LoadIMAGE Exception" + e);
            res = 1;
        };

        if (res==1)return 1;
        try{
            AbortCheck();
            ClearFreeTextLines();
            if (OpenTextDocument()==0){  //sendCommand(CMD_DBEG);
                sendCommand(CMD_GCPR+"000");
                res = CloseTextDocument();
            }

        }catch (Exception e){
            return 1;
        }

        return res;
    }


    /**
     * Загрузка QR-кода
     * <br>
     * <br>
     * @param scale             множитель масштаба изображения 2..10
     * @param LevelCorrection  Символ идентификатор уровня коррекции ошибок
     *                          [‘L’, ‘M’, ‘Q’, ‘H’]. Значение параметра вне указанного множества автоматически заменяется на ‘L’
     * @param StringForQR       Строка данных до 240 символов для кодирования
     * @return                  возвращает 0-успех,1-ошибки протокола, 2-неверные параметры
     */
    @MethodInfo(idMetod = 92, Params = {"scale","LevelCorrection","StringForQR"})
    public  int LoadQR (int scale, String LevelCorrection, String StringForQR ) throws MariaException {

        String strQr = (StringForQR.trim().length()>240)?StringForQR.trim().substring(0,240):StringForQR;
        if (!isConvertableToNumeric(scale))return 2;
        if ((scale<2)&&(scale>10))return 2;
        String res = sendCommand(CMD_GCQR+String.format("%02d%1s%s",scale,LevelCorrection,strQr));
        return (res.contains(ANSW_READY)?0:1);
    }

    /**
     * Печатает QR-код
     * <br>
     * <br>
     * @param placeForPrint     0/1  признак расположения текста до/после фискальной информации
     * @return                  возвращает 0-успех,1-ошибки протокола, 2-неверные параметры
     */
    @MethodInfo(idMetod = 105, Params = {"placeForPrint"})
    public  int PrintQR (int placeForPrint ) throws MariaException {
        if ((placeForPrint<0)&&(placeForPrint>1))return 2;
        return sendCommand(CMD_GCPR+String.format("%1d00",placeForPrint)).contains(ANSW_READY)?0:1;
    }



    /**
     * Загружает логотип в ЭККА из файла
     * <br>
     * <br>
     * @param path              Путь к файлу
     * @param sizeOption        0/1/2  без изменений/растянуть/подогнать под допустимый размер
     * @return                  возвращает 0 в случае успеха
     */
    @MethodInfo(idMetod = 93, Params = {"path","sizeOption"})
    public int SetLogoFromFile (String path, int sizeOption) throws MariaException {
        if (!isConvertableToNumeric(sizeOption)) return 2;

        int res = 0;

        try {
            File file = new File(path);//"/mnt/internal_sd/Pictures/image.png");
            if (!file.exists()) {
                Log.e(DBG_TAG, "SetLogoFromFile file [" + file.getCanonicalPath() + "] - is not EXIST");
                return 1;
            }
           // Log.d(DBG_TAG, "SetLogoFromFile: ["+path+"]"+sizeOption);

            ClassImg img = new ClassImg();

            ClassImg.RequestSizeOptions options = ClassImg.RequestSizeOptions.RESIZE_EXACT;
            switch (sizeOption){
                case 0:
                    options = ClassImg.RequestSizeOptions.RESIZE_EXACT;//без изменений
                    break;
                case 1:
                    options = ClassImg.RequestSizeOptions.RESIZE_FIT;//растянуть
                    break;
                case 2:
                    options = ClassImg.RequestSizeOptions.RESIZE_INSIDE; // подогнать под допустимый размер
                    break;
            }


            byte[][] qrBuf = img.ConvertBitmapToArray(file, options);
            if (qrBuf == null) return 1;

            int CountRows = qrBuf.length;
            int CountBytesHorizontal = qrBuf[0].length;

            String tmpRes = "";

            for (int rows = 0;rows<CountRows;rows++){
                String str = "";
                for (int hor = 0;hor<CountBytesHorizontal;hor++) {
                    str += String.format("%02X", qrBuf[rows][hor]);
                }
                tmpRes=sendCommand("LUPC"+String.format("%03d",rows+1) + str);
                if(!tmpRes.trim().contains(ANSW_READY))return 1;
            }
            tmpRes=sendCommand("PUPC"+String.format("%03d",CountRows) );
            if(!tmpRes.trim().contains(ANSW_READY))return 1;

            tmpRes=sendCommand("AUPC"+String.format("%03d",CountRows) );
            if(!tmpRes.trim().contains(ANSW_READY))return 0;

        }catch (Exception e){
            Log.e(DBG_TAG, "SetLogoFromFile Exception" + e);
            res = 1;
        };
        return res;
    }


    /**
     * Отменяет печать логотипа на чеках
     * <br>
     * <br>
     * @return                  возвращает 0 в случае успеха
     */
    @MethodInfo(idMetod = 94, Params = {""})
    public int ClearLogo () throws MariaException {

        return (sendCommand("AUPC"+String.format("%03d",0)).trim().contains(ANSW_READY))?0:1;
    }


 /*********************************************************************************************
 *                                      Печать отчётов
 * ********************************************************************************************/
    /**
     * Реализация товаров и услуг в разрезе артикулов.
     * <br>
     * <br>
     * Перед выполнением Z-отчета с обнулением (функция createZReport) рекомендуется
     * вызвать sellsByArticle для получения печатного отчета.
     *
     * @return                         возвращает 0 в случае успеха
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 20, Params = "")
    public int ArticleReport () throws MariaException{
        return(sendCommand(CMD_ARTZ).trim().contains(ANSW_READY))?0:1;

    }
    /**
     * Примененные скидки и надбавки.
     * <br>
     * <br>
     * Если при регистрации реализации (возврата) товаров применялись скидки
     * или надбавки, то перед выполнением Z-отчета с обнулением (команда createZReport)
     * рекомендуется вызвать getAppliedDiscountsAndSurcharges для получения печатного отчета.
     *
     * @return                         возвращает 0 в случае успеха
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 21, Params = "")
    public int DiscountReport() throws MariaException{
        return (sendCommand(CMD_DIZV).trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Периодический отчет ФП (по номерам).
     * <br>
     * <br>
     * Без параметров вызывает генерацию отчета за весь период эксплуатации ЭККР.
     * В случае, если между Z-отчетами с номерами startZReport и endZReport производилась
     * смена валюты ЭККР, более 300 изменений схем налогообложения, замена фискального
     * (регистрационного) номера ЭККР, то печатается несколько
     * отчетных чеков, соответствующих периодам с одинаковой валютой, 300 наборами
     * схем налогообложения, с одним и тем же регистрационным номером ЭККР.
     *
     *
     * @param  startZReport           int 4 символов номер начального Z-отчета.
     * @param  endZReport             int 4 символов номер конечного Z-отчета.
     * @param  print_full_report      boolean falase/true сокращенный/полный
     *
     * @return                        возвращает 0 в случае успеха
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 22, Params = {"startZReport","endZReport","print_full_report"})
    public int  PeriodicalFiscalReport(int startZReport, int endZReport, boolean print_full_report) throws MariaException{
        int result=1;
        String tmpRes = "";
    if (isConvertableToNumeric(startZReport))

        if (print_full_report)
            tmpRes = sendCommand(CMD_IREN + String.format("%04d%04d",startZReport, endZReport));
        else
            tmpRes = sendCommand(CMD_FIRN + String.format("%04d%04d",startZReport, endZReport));

        if (tmpRes.trim().contains(CMD_NREP)) {
            result = 0;
        }
        return result;
    }

    /**
     * Печатает X-отчёт
     * <br>
     * <br>
     * @param  printFundsFlow   0/1 признак печати в X- отчете  сведений о движении средств
     *
     * @return                        возвращает 0 в случае успеха
     */
    @MethodInfo(idMetod = 23, Params = {"printFundsFlow"})
    public int XReport(boolean printFundsFlow) throws MariaException{
        String randomSymbol = "";
        if(printFundsFlow)
            randomSymbol = "1";

        return ((sendCommand(CMD_ZREP + randomSymbol).trim().contains(CMD_ZREP))?1:0);
    }

    /**
     * Печатает Z-отчёт
     * <br>
     * <br>
     *
     * @return                        возвращает 0 в случае успеха
     */
    @MethodInfo(idMetod = 24, Params = "")
    public int ZReport() throws MariaException{

        return ((sendCommand(CMD_NREP).trim().contains(CMD_NREP))?0:1);
    }

    /**
     * Периодический  отчет ФП (по датам).
     * <br>
     * <br>
     * Без параметров вызывает генерацию отчета за весь период эксплуатации ЭККР.
     * В случае, если между датами dateBegin и dateEnd производилась
     * смена валюты ЭККР, более 300 изменений схем налогообложения, замена фискального
     * (регистрационного) номера ЭККР, то печатается несколько
     * отчетных чеков, соответствующих периодам с одинаковой валютой, 300 наборами
     * схем налогообложения, с одним и тем же регистрационным номером ЭККР.
     *
     * @param  dateBegin              String- 8 символов дата начала отчета в формате ггггммдд;
     * @param  dateEnd                String - 8 символов дата конца отчета в формате ггггммдд;
     * @param  print_full_report      boolean falase/true сокращенный/полный
     *
     * @return                        int 0
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 25, Params = {"dateBegin","dateEnd","print_full_report"})
    public int PeriodicalFiscalReportDate(String dateBegin, String dateEnd, boolean print_full_report) throws MariaException{
        int result=0;
        String tmpRes = "";

        if (print_full_report)
            tmpRes = sendCommand(CMD_IREP + dateBegin + dateEnd);
        else
            tmpRes = sendCommand(CMD_FIRP + dateBegin + dateEnd);

        if (tmpRes.trim().contains(ANSW_READY)) {
            result = 1;
        }
        return result;


    }


    /**
     * Состояние аппаратуры.
     *
     * @return                        int 0
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 80, Params = "")
    public int GetHardwareStatus() throws MariaException{
        sendCommand("HDZV0");
        return 0;
    }


    /**********************************************************************
     *                   Управление механизмами и доп устройствами
     *********************************************************************/



    /**
     * Выдача информации на выносной индикатор.
     *
     * @param  symbols                String до 200 произвольных символов с соблюдением ограничений
     *                                транспортного протокола – данные для передачи по каналу связи на выносной индикатор.
     *                                В качестве данных ВУ формирует последовательности символов
     *                                в соответствии с протоколом конкретного индикатора.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 49, Params = {"symbols" })
    public int PutToExternalDisplay(String symbols) throws MariaException{
        String tmpRes = sendCommand(CMD_DIsp + symbols);
        return (tmpRes.trim().contains(ANSW_READY))?0:1;
    }
    /**
     * Обеспечение совместимости выдачи информации на встроенный
     * 10-ти разрядный LED (или 16-ти разрядный LCD) индикатор ЭККР предыдущих моделей.
     * <br>
     * <br>
     * В нормальном состоянии на дисплее отображается текущее время. После
     * вызова setPrintOnLEDandLCDLegacy с непустыми messageNumber,decimal, image на дисплее удерживается
     * переданная информация. Вызовом функции setPrintOnLEDandLCDLegacy без параметров дисплей переводится
     * в нормальное состояние.
     *
     * @param  messageNumber          int 1 символ [’1’,'2',’3’]:
     *                                - номер зарезервированного сообщения на первой строке дисплея
     *                                («Цiна», «Сума» или «Здача» соответственно).
     *                                При значении параметра равном ‘0’ ни одно сообщение не выводится.
     *
     *
     * @param  sum                   сумма отображается на второй строке индикатора.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 50, Params = {"messageNumber", "sum" })
    public int PutSumToDisplay(int messageNumber, double sum) throws MariaException{

        if (!isConvertableToNumeric(sum)) return 1;
        int scale = BigDecimal.valueOf(sum).scale();
        if (scale>9) return 1;

        if (((sum -(int)sum)>0)&&(scale>0)) {
            for (int i=0;i<scale;i++)
                sum*=10;
        }
        if (scale>0)scale++;//костыль для соблюдения протокола

        String strData = "DISp" + messageNumber + scale + String.format("%10d", (int)sum);
        //Log.d(DBG_TAG, "PutSumToDisplay " + scale+">>"+sum+">"+strData);
        return (sendCommand(strData).contains(ANSW_READY)?0:1);
    }



    /**
     * Управляет работой обрезчика чековой ленты и звуковым сигналом
     * <br>
     * <br>
     * В нормальном состоянии на дисплее отображается текущее время. После
     * вызова setPrintOnLEDandLCDLegacy с непустыми messageNumber,decimal, image на дисплее удерживается
     * переданная информация. Вызовом функции setPrintOnLEDandLCDLegacy без параметров дисплей переводится
     * в нормальное состояние.
     *
     * @param  isCutterEnabled       включает(1) или выключает(0) автоматическую обрезку чековой ленты после
     *                               завершения печати чека
     * @param  isBeepEnabled         включает(1) или выключает(0) функцию звукового сигнала после завершения
     *                               печати чека
     *
     *
     * @param  partialCutOn          включает(1) или включает(0) функцию неполной обрезки чековой ленты.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 26, Params = {"isCutterEnabled", "isBeepEnabled", "partialCutOn"})
    public int CutAndBeep(boolean isCutterEnabled, boolean isBeepEnabled, boolean partialCutOn) throws MariaException{
        String randomSymbol = partialCutOn ? "1" : "";
        String strData = CMD_CUTR + ((isCutterEnabled)?"1":"0") + ((isBeepEnabled)?"1":"0") + randomSymbol;
        return (sendCommand(strData).trim().contains(CMD_CUTR)?0:1);
    }


    /**
     * Открытие кассового ящика.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 34, Params = "")
    public int OpenCashBox() throws MariaException{

     return(sendCommand(CMD_KASS).trim().contains(ANSW_READY)?0:1);

    }


    /**********************************************************************
     *                   Настройка параметров
    *********************************************************************/

    /*
     * Определяет режим печати информации о наложенных налогах
     *
     * @param  on         1 - наложенные налоги печатаются после каждой товарной строки в чеке, 0 - сумма
     *                        наложенных налогов единожды печатается перед строкой "РАЗОМ"
     *
     * @return               возвращает 0 в случае успеха, пробрасывает исключение
     *                      MariaException в случае неудачи
     * */
    @MethodInfo(idMetod = 18, Params = {"on"})
    public int SetOnSaleTaxes ( int on ) throws MariaException{

        return (sendCommand(CMD_PPOD + ((on==1)?"1":"")).contains(CMD_PPOD)?0:1);
    }

    /**
     * Управление определением налогооблагаемого оборота при применении двух налогов.
     * <br>
     * <br>
     * В обычном режиме налогооблагаемый оборот для двух схем налогообложения
     * определяется независимо от порядка применения таких схем следующим образом:
     * - Пусть процентная ставка одного налога A%, второго налога B%,
     * сумма, указанная в inputCheckFiscalDataRegisterNew(inputCheckFiscalDataRegisterNewRefund), inputCheckFiscalDataByCodes
     * (inputCheckFiscalDataByCodesRefund) или
     * inputCheckFiscalDataUseProgrammed(inputCheckFiscalDataUseProgrammedRefund) - S.
     * Тогда оборот для налога A равен S-
     * ((S*B)/(100+A+B)), оборот для налога B равен
     * S - ((S*A)/(100+A+B)).
     *
     * Функция применяется непосредственно перед вызовом
     * inputCheckFiscalDataRegisterNew(inputCheckFiscalDataRegisterNewRefund),
     * inputCheckFiscalDataByCodes(inputCheckFiscalDataByCodesRefund) или
     * inputCheckFiscalDataUseProgrammed(inputCheckFiscalDataUseProgrammedRefund), в которых предполагается
     * указать 2 схемы налогообложения одновременно.
     * Значение schemaId1 соответствует идентификатору одного из налогов (далее «Налог1»),
     * указываемых при следующем вызове inputCheckFiscalDataRegisterNew(inputCheckFiscalDataRegisterNewRefund),
     * inputCheckFiscalDataByCodes(inputCheckFiscalDataByCodesRefund)
     * или inputCheckFiscalDataUseProgrammed(inputCheckFiscalDataUseProgrammedRefund).
     * Значение schemaId2 соответствует идентификатору другого
     * налога (далее «Налог2»),
     * указываемого при следующем вызове inputCheckFiscalDataRegisterNew(inputCheckFiscalDataRegisterNewRefund),
     * inputCheckFiscalDataByCodes(inputCheckFiscalDataByCodesRefund) или
     * inputCheckFiscalDataUseProgrammed(inputCheckFiscalDataUseProgrammedRefund).
     * Налогооблагаемый оборот определяется следующим образом:
     * - Для Налога1 это сумма, указанная при вызове inputCheckFiscalDataRegisterNew(inputCheckFiscalDataRegisterNewRefund),
     * inputCheckFiscalDataByCodes(inputCheckFiscalDataByCodesRefund) или
     * inputCheckFiscalDataUseProgrammed(inputCheckFiscalDataUseProgrammedRefund)
     * - Для Налога2 это сумма, указанная при вызове inputCheckFiscalDataRegisterNew(inputCheckFiscalDataRegisterNewRefund),
     * inputCheckFiscalDataByCodes(inputCheckFiscalDataByCodesRefund) или
     * inputCheckFiscalDataUseProgrammed(inputCheckFiscalDataUseProgrammedRefund) минус сумма Налога1.
     *
     * Для изменения указанного выше порядка определения налогооблагаемого оборота
     * применяется команда changeTaxingOrder.
     *
     * @param  schemaId1              String 1 символ идентификатор (номер) первой применяемой схемы налогообложения
     *                                оборотов по реализации (возврата) товаров (услуг) ['А'..'Ж'] (КИРИЛЛИЦА).
     * @param  schemaId2              String 1 символ идентификатор (номер) второй применяемой схемы налогообложения
     *                                оборотов по реализации (возврата) товаров (услуг) ['А'..'Ж'] (КИРИЛЛИЦА).
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 11, Params = {"schemaId1","schemaId2"})
    public int SetDoubledTaxCalcMode (String schemaId1, String schemaId2) throws MariaException{
        return(sendCommand(CMD_NLPR + schemaId1 + schemaId2).contains(ANSW_READY)?0:1);

    }


    /**
     * Установка режима работы артикульной таблицы.
     * <br>
     * <br>
     * ЭККР обеспечивает учет реализации товаров(услуг) в разрезе их описаний
     * (АРТИКУЛОВ). Учет ведется по количеству и общей сумме реализации. Но-
     * мера артикулов от 1 до 15516. Возможны три режима работы артикульной
     * таблицы:
     * <br>
     * <br>
     * - «Регистрация новых».
     * <br>
     * <br>
     * В этом режиме после Z-отчета с обнулением
     * артикульная таблица очищается. Каждый вновь реализуемый товар с
     * новым 4-х разрядным кодом артикула (номером ячейки артикульной
     * таблицы) добавляет новую запись с описанием в таблицу. Все по-
     * следующие реализации товара с этим же кодом артикула учитываются
     * в этой записи таблицы. Для реализации-возврата товаров(услуг) в
     * этом режиме работы таблицы используются функции inputCheckFiscalDataRegisterNew и
     * inputCheckFiscalDataRegisterNewRefund.
     * <br>
     * <br>
     * - «Использование запрограммированных».
     * <br>
     * <br>
     * В этом режиме до начала реализации-возврата товара с данным кодом артикула необходимо
     * наличие в таблице артикулов записи с описанием этого товара.
     * Программирование артикула осуществляется функцией setGoodDescription. Для реа-
     * лизации-возврата товаров(услуг) в этом режиме работы таблицы ис-
     * пользуются функции inputCheckFiscalDataUseProgrammed и inputCheckFiscalDataUseProgrammedRefund.
     * <br>
     * <br>
     * - «Регистрация новых по бухгалтерским кодам».
     * <br>
     * <br>
     * В этом режиме после Z-отчета с обнулением артикульная таблица очищается. Каждый
     * вновь реализуемый товар с новым 9-ти разрядным бухгалтерским ко-
     * дом артикула (поисковым признаком) добавляет новую запись с опи-
     * санием в таблицу. Все последующие реализации товара с этим же
     * кодом артикула учитываются в этой записи таблицы. Для реализа-
     * ции-возврата товаров(услуг) в этом режиме работы таблицы исполь-
     * зуются функции inputCheckFiscalDataByCodes и inputCheckFiscalDataByCodesRefund.
     * <br>
     * <br>
     * Перевод таблицы артикулов из режимов «Регистрация новых (по бухгалтер-
     * ским кодам)» в режим «Использование запрограммированных» не производит
     * ее очистку и оставляет доступными (т.е. «запрограммированными») арти-
     * кулы, реализация которых была зарегистрирована в режиме «Регистрация
     * новых (по бухгалтерским кодам)».
     * Настройка сохраняется независимо от наличия электропитания до явного ее из-
     * менения вызывом  setArticleTableMode.
     * <br>
     * <br>
     * Заводская установка режима - «0 - Регистрация новых».
     *
     * @param  mode                   int 1 символ [0, 1, 2] признак режима «Регистрация новых»,
     *                                «Использование запрограммированных» или
     *                                «Регистрация новых по бухгалтерским кодам» соответственно.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */

    private int SetArticleMode (int mode) throws MariaException{
        return(sendCommand(CMD_ARMO + mode).trim().contains(ANSW_READY)?0:1);

    }
    /**
     * Программирование необязательной заключительной строки на чеке.
     * <br>
     * <br>
     * Настройка сохраняется независимо от наличия электропитания до явного ее изменения вызовом функции setCheckBottomString.
     *
     * @param  bottom                 String (не обязателтный параметр) до 43-х символов. Символы, переданные в bottom,
     *                                печатаются заключительной строкой на чеке
     *                                перед информацией о дате/времени печати.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
        @MethodInfo(idMetod = 28, Params = {"bottom"})
        public int SetCheckBottomLine(String bottom) throws MariaException{
            return (sendCommand(CMD_BOTM + bottom).trim().contains(ANSW_READY)?0:1);
        }

    /**
     * Программирование нескольких необязательных заключительных строк на чеке.
     * <br>
     * <br>
     * Без параметров – деактивация всех десяти строк заключительной информации.
     * С единственным параметром lastStringNumber – деактивация одной строки заключитель-
     * ной информации с номером lastStringNumber.
     * Строка с номером lastStringNumber будет активирована для печати при наличии всех параметров.
     * На значение параметра strToPrint накладываются ограничения: никакая строка
     * strToPrint с fontModifier равным “1”, содержащая подстроку ‘ФI’, не будет отпечатана с удвоенной шириной.
     * При использовании фискального модуля в составе ЭККА будут отпечатаны
     * первые 21 символов строки strToPrint при значении fontModifier равном ‘1’.
     * При использовании фискального модуля в составе РРКО будут отпечатаны
     * первые 30 (при fontModifier==’0’) или 15 (при fontModifier==’1’) символов строки strToPrint.
     * Настройки сохраняются независимо от наличия электропитания до явного их изменения вызовом setCheckBottomStrings.
     *
     * @param  lastStringNumber       String  1 символ [“0”..”9”] номер заключительной строки сверху вниз.
     * @param  fontModifier           String  (не обязателтный параметр) 1 символ [“0”..”3”] признак печати строки модифицированным шрифтом:
     *                                  “0” – нет модификации,
     *                                  “1” - c удвоенной шириной,
     *                                  “2” – с удвоенной высотой,
     *                                  “3” - c удвоенными шириной и высотой.
     * @param  strToPrint             String  (не обязателтный параметр) до 43-х символов собственно строка для печати.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 29, Params = {"lastStringNumber", "fontModifier", "strToPrint"})
    public int SetCheckBottomLineEx(String lastStringNumber,  String fontModifier, String strToPrint) throws MariaException{
        String randomSymbol = "0";
        String strData = CMD_BOTm + lastStringNumber + randomSymbol + fontModifier + strToPrint;
        return (sendCommand(strData).trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Программирование заголовочной информационной строки на чеке.
     * <br>
     * <br>
     * Настройка сохраняется независимо от наличия электропитания до явного ее изменения вызовом функции setCheckHeaderString.
     *
     * @param  header                 String (не обязателтный параметр)до 43-х символов.
     *                                Символы, переданные в header, печатаются первой строкой на чеке.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 30, Params = {"header"})
    public int SetCheckHeadLine(String header) throws MariaException{
        int result=0;
        String tmpRes = sendCommand(CMD_HEAD + header);
        return  (sendCommand(CMD_HEAD + header).trim().contains(ANSW_READY)?0:1);
    }


    /**
     * Программирование мнемоники торгового отдела.
     * <br>
     * <br>
     * По умолчанию после строки с номером чека на документе печатается строка
     * вида "Вiдд.XXXXXXXXXXXXXXXX Касир YYYYYYYYY". В зависимости от специфики
     * торгового предприятия командой 'DEPT' измените символы "Вiдд." на требуемые ("Окно", "Терм" и т.д.).
     * Настройка сохраняется независимо от наличия электропитания до явного ее изменения вызовом функции
     * programMnemonicsOfTradingDepartment.
     * Заводская установка – "Вiдд.".
     *
     * @param  MnemonicValue          String (не обязательный параметр) до 5-ти символов.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 31, Params = {"MnemonicValue"})
    public int SetDeptAlias(String MnemonicValue) throws MariaException{
        return(sendCommand(CMD_DEPT + MnemonicValue).trim().contains(ANSW_READY)?0:1);
    }



    /**
     * Программирование cхемы налогообложения.
     * <br>
     * <br>
     * @param  TaxSchemeNumber        int номер схемы налогообложения[1..7] соответственно cхем налогообложения [A..Ж]
     * @param  TaxType                int 0/1/2 соответственно вложенный/наложенный с добавлением/наложенный с вычитанием (“подоходный”)
     * @param  TaxRate                double процентная ставка налога
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 115, Params = {"TaxSchemeNumber", "TaxType", "TaxRate"})
    public int SetTaxScheme(int TaxSchemeNumber,int TaxType, double TaxRate) throws MariaException{

        if ((!isConvertableToNumeric(TaxSchemeNumber))||
                (!isConvertableToNumeric(TaxRate)) ||
                (!isConvertableToNumeric(TaxType))) return 1;

        if ((TaxSchemeNumber>7)||(TaxSchemeNumber<1)) return 1;
        if ((TaxType>2)||(TaxType<0)) return 1;
        if ((TaxRate>99.99)||(TaxRate<0)) return 1;
      //  Log.d(DBG_TAG, ">>>SetTaxScheme: "+CMD_NALG + String.format("%c%d%04d",TaxName[TaxSchemeNumber-1],TaxType, (int)(TaxRate*100)));
        return(sendCommand(CMD_NALG + String.format("%c%d%04d",TaxName[TaxSchemeNumber-1],TaxType, (int)(TaxRate*100))).trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Установка наименования налога.
     * <br>
     * <br>
     * @param  TaxSchemeNumber        int номер схемы налогообложения [1..7] соответственно cхем налогообложения [A..Ж]
     * @param  TaxSchemeName          String до 19-ти символов наименование налога
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 116, Params = {"TaxSchemeNumber", "TaxSchemeName"})
    public int SetTaxName(int TaxSchemeNumber,String TaxSchemeName) throws MariaException{

        if ((!isConvertableToNumeric(TaxSchemeNumber))) return 1;
        if ((TaxSchemeNumber>7)||(TaxSchemeNumber<1)) return 1;

        return(sendCommand(CMD_NNAM + String.format("%c%s",TaxName[TaxSchemeNumber-1], (TaxSchemeName.trim().length()<=19?TaxSchemeName:TaxSchemeName.substring(0,19)))).trim().contains(ANSW_READY)?0:1);
    }

    /**
     * Управление печатью информации о скидках-надбавках в чеке.
     * <br>
     * <br>
     * Команда без параметра не изменяет настроек.
     * Применяется до открытия чека командой ‘PREP’.
     * Настройка сохраняется независимо от наличия электропитания до явного ее изменения
     * вызовом функции setupDiscountAllowancesInfoPrinting.
     * Заводская установка – ‘0’ (печать после каждой позиции).
     *
     * @param  value                  int  1 символ
     *                                ‘0’ – обычная печать информации о скидке-надбавке
     *                                после каждой фискальной позиции в чеке,
     *                                ‘1’ - печать информации об итоговой скидке-надбавке только в итоге чека.
     *
     * @return                        String возвращает 1 символ
     *                                ‘0’ – обычная печать информации о скидке-надбавке после каждой фискальной позиции в чеке,
     *                                ‘1’ - печать информации об итоговой скидке-надбавке только в итоге чека.
     */
    @MethodInfo(idMetod = 32, Params = {"value"})
    public String SetDiscountPrintMode (int value) throws MariaException{
        String str = sendCommand(CMD_NPDI + value);
        str = str.replaceFirst(CMD_NPDI, "");
        return str;
    }
    /**
     * Установка наименования итогов по скидокам-надбавкам в чеке.
     *
     * @param  discountTotalsName     String  22 символа - наименование итога по скидкам в пределах закрываемого чека.
     * @param  surchargesTotalsName   String  22 символа - наименование итога по надбавкам в пределах закрываемого чека.
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 33, Params = {"discountTotalsName","surchargesTotalsName"})
    public int SetDiscountUpcountTotalsName(String discountTotalsName, String surchargesTotalsName) throws MariaException{

        return (sendCommand(CMD_ZDNM + discountTotalsName + surchargesTotalsName).trim().contains(ANSW_READY)?0:1);
    }



    /**
     * Определение набора служебных сообщений на чеке.
     * <br>
     * <br>
     * Используйте команду перед открытием чека, т.е. до вызова openCheck.
     * <br>
     * <br>
     * В зависимости от специфики работы предприятия, реализующего товары
     * (услуги), меняется набор служебных сообщений на чеке, печатаемых перед каж-
     * дой фискальной частью (реализация и возврат):
     * <br>
     * <br>
     * +----------------------------------------+----------------------------------------------------------+
     * | Номер набора (специфика)               | Сообщение при РЕАЛИЗАЦИИ |     Сообщение при ВОЗВРАТЕ    |
     * +----------------------------------------+----------------------------------------------------------+
     * | '0' торговые ( розница, опт)           |            ----          |    "ПОВЕРНЕННЯ ПО ЧЕКУ N..."  |
     * +----------------------------------------+--------------------------+-------------------------------+
     * | '1' автостанции, вокзалы               |            ----          |    "ПОВЕРНЕННЯ ПО ЧЕКУ N..."  |
     * +----------------------------------------+--------------------------+-------------------------------+
     * | '2' услуги по кредитованию,            |         "ОДЕРЖАНО:"      |            "ВИДАНО:"          |
     * | ломбарды, игорный бизнес               |                          |                               |
     * | (только «Возврат»: операции возмещения |                          |                               |
     * | по фишкам и выплаты выигрыша)          |                          |                               |
     * +----------------------------------------+--------------------------+-------------------------------+
     * <br>
     * <br>
     * Настройка сохраняется независимо от наличия электропитания до явного
     * ее изменения новым вызовом setServiceMessagesKit.
     * Заводская установка режима – «'0' - торговые».
     *
     * @param  messagesKitNumber      int 1 символ в диапазоне [0..2] номер набора сообщений.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 51,Params = {"messagesKitNumber"})
    public int SetServiceMessagesKit(int messagesKitNumber) throws MariaException{
        return (sendCommand("TMOD" + messagesKitNumber).contains(ANSW_READY)?0:1);
    }

    /**
     * Управление видом представления фискальной информации на чеке.
     * <br>
     * <br>
     * Имеется возможность представлять фискальную информацию на чеке в таб-
     * личном виде, когда цена, количество и сумма по каждой позиции чека печата-
     * ются в соответствующих колонках таблицы. Включение табличного режима произ-
     * водится вызовом setFiscalInformationTableView с параметром true. Выключение - с параметром false.
     * Настройка сохраняется независимо от наличия электропитания до явного
     * ее изменения новым вызовом setFiscalInformationView.
     * Заводская установка режима – «НЕ таблица».
     *
     * @param  isTableView            boolean Включение/Выключение табличного режима
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 52,Params = {"isTableView"})
    public int SetCheckAsTable(boolean isTableView) throws MariaException{
        String str = !isTableView ? "TABL" + "1" : "TABL";
        //Log.d(MainActivity.DBG_TAG, "SetFiscalInformationTableView:  "+str);
        return ((sendCommand(str).contains(ANSW_READY))?0:1);
    }

    /**
     * Установка режима «построчной» печати чека.
     * <br>
     * <br>
     * Применяется до открытия чека вызовом openCheck.
     * Действует только в пределах одного чека.
     * После каждой команды создания фискальной позиции чека (
     *  inputCheckFiscalDataRegisterNew(inputCheckFiscalDataRegisterNewRefund),
     *  inputCheckFiscalDataByCodes(inputCheckFiscalDataByCodesRefund) или
     *  inputCheckFiscalDataUseProgrammed(inputCheckFiscalDataUseProgrammedRefund)
     * )
     * производится полная выгрузка буфера пе-
     * чати с ожиданием окончания физического процесса печати и контролем исправ-
     * ности принтера (в т.ч. наличия бумаги).
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 53,Params = "")
    public int SetLinePrintingOn() throws MariaException{
        return (sendCommand("STFL").contains(ANSW_READY)?0:1);

    }




    /**
     * Корректировка часов реального времени.
     * <br>
     * <br>
     * Команду можно выполнить 1 раз не более +/- 90 минут оттекущего времени после Z-отчета с обнулением.
     *
     * @param  hours
     * @param  minutes
     * @param  seconds
     *
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 37, Params = {"hours", "minutes", "seconds"})
    public int SetTime(int hours, int minutes, int seconds) throws MariaException{
        if ((!isConvertableToNumeric(minutes))||
             (!isConvertableToNumeric(minutes))||
                (!isConvertableToNumeric(seconds))) return 2;

        return(sendCommand(CMD_CTIM + String.format("%02d:%02d:%02d", hours, minutes, seconds)).contains(ANSW_READY)?0:1);
    }

    /**
     * Корректировка часов реального времени – плюс 1 час.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 38, Params = "")
    public int SetTimePlusOneHour() throws MariaException{
        String tmpRes =  sendCommand(CMD_CTMP);
        return (tmpRes.trim().contains(ANSW_READY))?0:1;
    }


    /**
     * Корректировка часов реального времени – минус 1 час.
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 39, Params = "")
    public int SetTimeMinusOneHour() throws MariaException{
        String tmpRes =   sendCommand(CMD_CTMM);
        return (tmpRes.trim().contains(ANSW_READY))?0:1;
    }
/*************************************************************************************
 *                                  Информация
 *************************************************************************************/
    /**
     * Запрос регистра учета реализации артикула.
     *
     * @param  articleNumber           int - номер артикула в диапазоне 1-15516 или
     *
     *                                 - номер артикула по бухгалтерской (внутрисистемной)
     *                                 кодировке в диапазоне 1..999999999 - только в режиме работы артикульной
     *                                 таблицы «Регистрация новых по бухгалтерским кодам» (isRegNewByCodesEnabled == true)
     *

     *
     * @return                        возвращает HashMap<String, String>:
     *                                - Ключ: articleNumberRes, значение: номер запрошенного артикула;
     *
     *                                - Ключ: goodName, значение: наименование товара(24 символа);
     *
     *                                - Ключ: isDivisible, значение: 0 - делимый, 1 - неделимый
     *
     *                                - Ключ: taxingState, значение: 8 символов состояние налогообложения. Каждая позиция слева
     *                                направо соответствует схемам налогообложения от ‘А’ до ‘З’. Если данная
     *                                схема налогообложения не используется, в соответствующей позиции
     *                                находится символ ‘0’. В противном случае в соответствующей позиции
     *                                находится соответствующий символ [‘А’..‘З’].
     *                                Схема 'З' не программируется по setUpSchemesOfTaxation,
     *                                всегда активна со значениями:
     *                                тип «вложенный» (0), ставка 0,00% (0000).
     *                                Указание на использованиесхемы ‘З’ подразумевает регистрацию операции такой,
     *                                что не являетсяобъектом налогообложения;
     *
     *                                - Ключ: quantityOfGoodsSoldByArticle, значение: 10 символов в формате XXXXXX.XXXX:
     *                                  количество реализованного товара с данным артикулом;
     *
     *                                - Ключ: totalGoodsSoldByArticle, значение: 10 символов общая сумма реализации товара
     *                                  с данным артикулом в копейках;
     *
     *                                - Ключ: cashlessPayment, значение: 9 символов код артикула по бухгалтерской (внутрисистемной)
     *                                  кодировке в диапазоне ["000000001".."999999999"]. Передается только в
     *                                  режиме работы артикульной таблицы
     *                                  «Регистрация новых по бухгалтерским кодам» (isRegNewByCodesEnabled == true);
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 40, Params = {"articleNumber"})
    public HashMap<String, String> GetArticleInfo(int articleNumber) throws MariaException{

        String _articleNumber = String.format("%09d", articleNumber);

        String res = sendCommand(CMD_ARTD + _articleNumber).replaceFirst(CMD_ARTD, "");

        HashMap<String, String> result = new HashMap<>();

        int lastPos = 0;
        String articleNumberRes = res.substring(lastPos, lastPos + 4);
        articleNumberRes = getDecArticleNumber(articleNumberRes) + "";
        result.put("articleNumberRes", articleNumberRes);
        lastPos += 4;

        result.put("goodName", res.substring(lastPos, lastPos + 24));
        lastPos += 24;

        result.put("isDivisible", res.substring(lastPos, lastPos + 1));
        lastPos += 1;

        result.put("taxingState", res.substring(lastPos, lastPos + 8));
        lastPos += 8;

        result.put("quantityOfGoodsSoldByArticle", res.substring(lastPos, lastPos + 10));
        lastPos += 10;

        result.put("totalGoodsSoldByArticle", res.substring(lastPos, lastPos + 10));
        lastPos += 10;

      /*  if(isRegNewByCodesEnabled)
            result.put("cashlessPayment", res.substring(lastPos, lastPos + 9));*/

       /* Log.d(DBG_TAG, "ARTD res: " + res);
        for (HashMap.Entry<String, String> entry : result.entrySet())
        {
            Log.d(DBG_TAG, "HashMap result key: "   + entry.getKey());
            Log.d(DBG_TAG, "HashMap result value: " + entry.getValue());
        }*/


        return result;
    }

    /**
     *Позволяет определить состояние фискальной смены ЭККА
     *
     * @return                        0 (произошла ошибка), 1 (смена закрыта), 2 (смена открыта)
     */

    @MethodInfo(idMetod = 41, Params = {""})
    public  int GetBusinessDayState ()throws MariaException{
        Map<String,String> ConfigEkka = GetPrinterConfig ();
        int isReported = Integer.parseInt(ConfigEkka.get("isReported"));

        return ((isReported==0)?2:1);
    }



    /**
     * Запрос информации о движении средств по кассе.
     * <br>
     * <br>
     * Показывает актуальное (при currentOrPrev == 0) или предыдущее (при currentOrPrev == 1) состояние
     * регистров учета движения денежных средств, изменяемых операциями реализации-возврата
     * или служебным внесением-изъятием денежных средств.
     *
     * @param  currentOrPrev          int - 0 или 1 – признак запроса данных «по текущей смене»
     *                                или «по предыдущей смене» соответственно.
     *
     * @return                        возвращает HashMap<String, String>:
     *                                - Ключ: initialBalance, значение: "Початковий Залишок";
     *
     *                                - Ключ: serviceEntry, значение: "Службове Внесення";
     *
     *                                - Ключ: serviceRemoval, значение: "Службове Вилучення";
     *
     *                                - Ключ: obtained, значение: "Одержано";
     *
     *                                - Ключ: issued, значение: "Видано";
     *
     *                                - Ключ: finalBalance, значение: "Кiнцевий залишок";
     *
     *                                - Ключ: cashlessPayment, значение: "Безготiвкова оплата";
     *
     *                                - Ключ: cashlessRefund, значение: "Безготiвкове повернення";
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 42, Params = {"currentOrPrev"})
    public HashMap<String, String> GetCashInfo(int currentOrPrev) throws MariaException{
        String[] keys = {"initialBalance", "serviceEntry", "serviceRemoval", "obtained",
                "issued", "finalBalance", "cashlessPayment", "cashlessRefund"};

        String res = sendCommand(CMD_CCAS + currentOrPrev);
        HashMap<String, String> result = new HashMap<>();

        if (res.contains(CMD_CCAS)) {
            res = res.replaceFirst(CMD_CCAS, "");

            int lastPos = 0;
            for (int i = 0; i < 8; i++) {
                result.put(keys[i], res.substring(lastPos, lastPos + 11));
                lastPos += 11;
            }
        }
      /*  Log.d(DBG_TAG, "CCAS res: " + res);
        for (HashMap.Entry<String, String> entry : result.entrySet())
        {
            Log.d(DBG_TAG, "HashMap result key: "   + entry.getKey());
            Log.d(DBG_TAG, "HashMap result value: " + entry.getValue());
        }*/


        return result;
    }


    /**
     * Запрос текущего состояния дневных фискальных регистров.
     * <br>
     * <br>
     * Схема 'З' не программируется по setUpSchemesOfTaxation, всегда активна со значениями:
     * тип «вложенный» (0), ставка 0,00% (0000). Указание на использование
     * схемы ‘З’ подразумевает регистрацию операции такой, что не является
     * объектом налогообложения.
     *
     * @return                        возвращает HashMap<String, String>:
     *                                - Ключ: totalSalesTurnover, значение: общий оборот реализации;
     *
     *                                - Ключ: salesAmountTurnoverBySchemesTaxation1..salesAmountTurnoverBySchemesTaxation7,
     *                                  значение: суммы оборотов реализации по схемам налогообложения, соответствующих номерам ['А'..'З'];
     *
     *                                - Ключ: nonTaxableSales, значение: не облагаемый налогом оборот реализации;
     *
     *                                - Ключ: totalRefundTurnover, значение: общий оборот возврата;
     *
     *                                - Ключ: refundAmountTurnoverBySchemesTaxation1..refundAmountTurnoverBySchemesTaxation7,
     *                                  значение: 12-ти символьные суммы оборотов возврата по схемам налогообложения,
     *                                  соответствующих номерам ['А'..'З'];
     *
     *                                - Ключ: totalRefundTurnover, значение: не облагаемый налогом оборот возврата;
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 43, Params = "")
    public HashMap<String, String> GetFiscalInfo() throws MariaException{
        String res = sendCommand(CMD_CFIS).replaceFirst(CMD_CFIS, "");
        HashMap<String, String> result = new HashMap<>();
        result.put("totalSalesTurnover", res.substring(0, 12));

        int lastPos = 12;
        for(int i = 1; i < 8; i ++){
            result.put("salesAmountTurnoverBySchemesTaxation" + i, res.substring(lastPos, lastPos + 12));
            lastPos += 12;
        }

        lastPos += 12;
        result.put("nonTaxableSales", res.substring(lastPos, lastPos + 12));
        lastPos += 12;

        result.put("totalRefundTurnover", res.substring(lastPos, lastPos + 12));
        lastPos += 12;

        for(int i = 1; i < 8; i ++){
            //lastPos = 12 * i + 12;
            result.put("refundAmountTurnoverBySchemesTaxation" + i, res.substring(lastPos, lastPos + 12));
            lastPos += 12;
        }

        lastPos += 12;
        result.put("nonTaxableRefundTurnover", res.substring(lastPos, lastPos + 12));

        Log.d(DBG_TAG, "CFIS res: " + res);
     /*   for (HashMap.Entry<String, String> entry : result.entrySet())
        {
            Log.d(DBG_TAG, "HashMap result key: "   + entry.getKey());
            Log.d(DBG_TAG, "HashMap result value: " + entry.getValue());
        }*/


        return result;
    }

    /**
     * Запрос номеров чеков, документов, идентификатора транзакции.
     *
     * @return                        возвращает HashMap<String, String>:
     *                                - Ключ: lastCheck, значение: 10 символов – номер последнего закрытого (или текущего открытого)
     *                                  чека реализации-возврата (продажи-покупки);
     *
     *                                - Ключ: lastDocDevice, значение: 10 символов – номер последнего закрытого или текущего открытого
     *                                  служебного документа, созданного (создаваемого) последовательностью
     *                                  команд (вызовов) от Android устройства;
     *
     *                                - Ключ: lastDocMaria, значение: 10 символов – номер последнего закрытого служебного документа,
     *                                  созданного ЭККР самостоятельно, как результат:
     *                                  - вызова performCashDepositOrWithdraw
     *                                  - выполнения служебных отчетов
     *                                  - самостоятельной печати служебных сообщений (включение ЭККР, смена времени и т.п.),
     *                                    содержащих строку «СЛУЖБОВИЙ ДОКУМЕНТ»
     *                                  Больший из номеров lastDocDevice,lastDocMaria является последним сквозным номером документа.
     *
     *                                - Ключ: articleTableMode, значение: 1 символ ‘0’, ‘1’ или ‘2’ - режим артикульной таблицы:
     *                                  «Регистрация новых»,
     *                                  «Использование запрограммированных» или
     *                                  «Регистрация новых»
     *                                  соответственно;
     *
     *                                - Ключ: isFiscalReportPerformed, значение: 1 символ - признак выполнения фискального отчета с обнулением
     *                                  "0" или "1" - не выполнен/выполнен;
     *
     *                                - Ключ: fiscalCheckNumber, значение: 4 символа - номер фискального отчетного чека
     *                                  (в зависимости от значения признака выполненного Z- отчета: либо номер последнего Z- отчета
     *                                  ("выполнен"), либо номер следующего Z- отчета ("не выполнен")).
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 44, Params = "")
    public HashMap<String, String> GetDocumentsInfo() throws MariaException{
        String res = sendCommand(CMD_GLCN).replaceFirst(CMD_GLCN, "");

        HashMap<String, String> result = new HashMap<>();

        int lastPos = 0;
        result.put("lastCheck", res.substring(lastPos, lastPos + 10));
        lastPos += 10;

        result.put("lastDocDevice", res.substring(lastPos, lastPos + 10));
        lastPos += 10;

        result.put("lastDocMaria", res.substring(lastPos, lastPos + 10));
        lastPos += 10;

        result.put("articleTableMode", res.substring(lastPos, lastPos + 1));
        lastPos += 2;

        result.put("isFiscalReportPerformed", res.substring(lastPos, lastPos + 1));
        lastPos += 1;

        result.put("fiscalCheckNumber", res.substring(lastPos, lastPos + 4));

        Log.d(DBG_TAG, "GLCN res: " + res);
       /* for (HashMap.Entry<String, String> entry : result.entrySet())
        {
            Log.d(DBG_TAG, "HashMap result key: "   + entry.getKey());
            Log.d(DBG_TAG, "HashMap result value: " + entry.getValue());
        }*/


        return result;
    }



    /**
     * Запрос даты-времени часов реального времени ЭККР.
     * <br>
     * <br>
     * Используйте эту команду для контроля состояния часов реального времени
     * ЭККР и для получения информации о времени для ВУ, не имеющих своих часов.
     *
     * @return                        возвращает HashMap<String, String>:
     *                                - Ключ: currentDate, значение: 8 символов - текущая дата
     *                                  (по системным часам реального времени) в формате ггггммдд;;
     *
     *                                - Ключ: currentTime, значение: 6 символов - текущее время
     *                                  (по системным часам реального времени) в формате ччммсс;
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 45, Params = "")
    public HashMap<String, String> GetDate() throws MariaException{
        String res = sendCommand(CMD_GETD).replaceFirst(CMD_GETD, "");

        HashMap<String, String> result = new HashMap<>();

        int lastPos = 0;
        result.put("currentDate", res.substring(lastPos, lastPos + 8));
        lastPos += 8;

        result.put("currentTime", res.substring(lastPos, lastPos + 6));

        Log.d(DBG_TAG, "GETD res: " + res);
       /* for (HashMap.Entry<String, String> entry : result.entrySet())
        {
            Log.d(DBG_TAG, "HashMap result key: "   + entry.getKey());
            Log.d(DBG_TAG, "HashMap result value: " + entry.getValue());
        }*/

        return result;
    }

    /**
     * Запрос списка запрограммированных схем налогообложения.
     *
     * @param  format           Если параметр format == false, то ответ на команду содержит информацию <c1>...<c8>, где:
     *                          <c1>...<c8> 14-ти символьные схемы вычисления налогов в формате
     *                          ггггммдднтсссс. Здесь:
     *                          ггггммдд - дата программирования
     *                          н ['1'..'8'] - номер схемы
     *                          т ['0'] -тип вложенный.
     *                          сссс - ставка в процентах с сотыми долями без десятичной точки.
     *                          Если параметр format == true ответ на команду содержит информацию <c1>...<c8>, где:
     *                          <c1>...<c8> 19-ти символьные наименования налогов, позиционно соответствующие схемам от ‘А’ до ‘З’.
     *
     * @return                  возвращает String спискок запрограммированных схем налогообложения в случае успеха,
     *                          пробрасывает исключение MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 46, Params = {"format"})

    public HashMap<String, String>  GetTaxesInfo(boolean format) throws MariaException{

       // List<Map<String , String>> taxes  = new ArrayList<Map<String,String>>();
        HashMap<String, String> taxes = new HashMap<>();

        String strData = format ? CMD_CNAL + "a" : CMD_CNAL;
        String res = sendCommand(strData);

        if (res.contains(CMD_CNAL)){
            int index = res.indexOf(CMD_CNAL);
            index = index+CMD_CNAL.length();
            res = res.substring(index);
            int lastPos = 0;
            try {
            for (int i=0;i<8;i++) {
               // Map<String, String> result = new HashMap<String, String>();
                taxes.put("progDate"+(i+1), res.substring(lastPos, lastPos + 8));

                lastPos += 8;
                taxes.put("taxNum"+(i+1), res.substring(lastPos, lastPos + 1));

                lastPos += 1;
                taxes.put("taxType"+(i+1), res.substring(lastPos, lastPos + 1));

                lastPos += 1;
                taxes.put("taxValue"+(i+1), res.substring(lastPos, lastPos + 4));

                lastPos += 4;
                //taxes.put(""+i,result);
            }
            }catch(Exception e){
                Log.e(DBG_TAG, "CNAL Exception: " + e.getMessage());
            }
        }else if  (res.contains(CMD_CNAM)){

            int index = res.indexOf(CMD_CNAM);
            index = index+CMD_CNAM.length();
            res = res.substring(index);
            res = String.format("%-152s",res);//если строка обрезана
            int lastPos = 0;
            try {
                for (int i = 0; i < 8; i++) {
                   // Map<String, String> result = new HashMap<String, String>();
                   // Log.d(DBG_TAG, "CNAM: [" + res.substring(lastPos, lastPos + 19)+"]");
                    taxes.put("taxName"+(i+1), res.substring(lastPos, lastPos + 19));
                   // taxes.add(i, result);
                    lastPos += 19;


                }
            }catch(Exception e){
                Log.e(DBG_TAG, "CNAM Exception: " + e.getMessage());
            }
        }


      /*  for (Map<String,String> m:taxes) {
            for (Map.Entry<String,String> e:m.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                Log.w(DBG_TAG, "key : " + key+" value:"+value);

            }
        }*/

        return taxes;
    }


    /**
     * Запрос внутреннего состояния ЭККР.
     *
     * @return                        возвращает HashMap<String, String>:
     *                                - Ключ: number, значение: 10 символов – последние 10 символов заводского номера
     *                                  (задан при изготовлении устройства);
     *
     *                                - Ключ: ownerNumber, значение: 10 символов - регистрационный номер
     *                                  (программируется вызовом registerOwnerInfo);
     *
     *                                - Ключ: enterpriseInfo, значение: 36 символов - наименование и адрес предприятия
     *                                  (программируется вызовом registerOwnerInfo);
     *
     *                                - Ключ: currentDate, значение: 8 символов - текущая дата
     *                                  (по системным часам реального времени) в формате ггггммдд;
     *
     *                                - Ключ: currentTime, значение: 6 символов - текущее время
     *                                  (по системным часам реального времени) в формате ччммсс;
     *
     *                                - Ключ: keyPosition, значение: 1 символ - положение системного ключа - зависит от положения ключа:
     *                                   “ОТКЛЮЧЕН”         (О) 0
     *                                   “РАБОТА”           (Р) 1
     *                                   “X- ОТЧЕТ”         (X) 2
     *                                   “Z-ОТЧЕТ”          (Z) 4
     *                                   “ПРОГРАММИРОВАНИЕ” (П) 8
     *
     *                               - Ключ: expectedCommand, значение: 1 символ - признак ожидаемого вызова функции создания документов
     *                                 (зависит от последней вызваной функции создания документов):
     *                                   openCheck                              1
     *                                   openServiceDocument                    2
     *                                   inputCheckFiscalDataRegisterNew        4
     *                                   closeCheck                             8
     *                                   inputCheckFiscalDataRegisterNewRefund 16
     *                                   ***
     *                                   (openCheck | openServiceDocument)      3

     *                               - Ключ: isCashierRegistered, значение: 1 символ - признак зарегистрированного кассира
     *                                 ("0"/"1" - не зарегистриран/зарегистрирован);
     *
     *                               - Ключ: cashierId, значение: 4 символа - идентификатор зарегистрированного кассира;
     *
     *                               - Ключ: isReported, значение: 1 символ - признак выполнения фискального отчета с обнулением
     *                                 (0/1 - не выполнен/выполнен, меняет свое значение на
     *                                  0 после подачи первой команды openCheck после выполнения Z- отчета);
     *
     *                               - Ключ: fiscalCheckNumber, значение: 12 символов - номер фискального отчетного чека
     *                                 (в зависимости от значения признака выполненного Z- отчета либо номер последнего Z- отчета
     *                                 ("выполнен"), либо номер следующего Z- отчета ("не выполнен")).
     *
     *                               - Ключ: lastCheckNumber, значение: 12 символов - номер последнего успешно созданного
     *                                 (или открытого по openCheck в данный момент) чека.

     *                               - Ключ: lastSuccessCommand, значение: 4 символа - идентификатор последней успешно исполненной команды;
     *
     *                               - Ключ: versionId, значение: 4 символа - идентификатор версии ПО ЭККР;
     *
     *                               - Ключ: versionDate, значение: 8 символов - дата создания версии ПО ЭККР в формате ггггммдд;
     *
     *                               - Ключ: currentHeader, значение: 18 символов - текущая информационная строка чека
     *                                 (совпадает с первыми 18-ю символами значения header,
     *                                 переданного в последней успешно исполненной команде setCheckHeaderString);
     *
     *                               - Ключ: setupCurrencyDate, значение: 8 символов - дата программирования валюты ЭККР в формате ггггммдд
     *                                 (совпадает со значением даты системных часов в момент программирования валюты вызовом setupCurrency);
     *
     *                               - Ключ: decimal, значение: 1 символ - количество знаков после десятичной точки в изображении
     *                                 сумм (совпадает со значением decimal, переданного при последнем успешном вызове setupCurrency);
     *
     *                               - Ключ: currencyName, значение: 3 символа - сокращенное наименование валюты ЭККР
     *                                 (совпадает со значением currencyName, переданного при последнем успешном вызове setupCurrency).
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 47, Params = "")
    public HashMap<String, String> GetPrinterConfig() throws MariaException{
        String res = sendCommand(CMD_CONf).replaceFirst(CMD_CONf, "");

        HashMap<String, String> result = new HashMap<>();

        int lastPos = 0;
        result.put("number", res.substring(lastPos, lastPos + 10));
        lastPos += 10;

        result.put("ownerNumber", res.substring(lastPos, lastPos + 10));
        lastPos += 10;

        result.put("enterpriseInfo", res.substring(lastPos, lastPos + 36));
        lastPos += 36;

        result.put("currentDate", res.substring(lastPos, lastPos + 8));
        lastPos += 8;

        result.put("currentTime", res.substring(lastPos, lastPos + 6));
        lastPos += 6;

        result.put("keyPosition", res.substring(lastPos, lastPos + 1));
        lastPos += 1;

        result.put("expectedCommand", res.substring(lastPos, lastPos + 1));
        lastPos += 1;

        result.put("isCashierRegistered", res.substring(lastPos, lastPos + 1));
        lastPos += 1;

        result.put("cashierId", res.substring(lastPos, lastPos + 4));
        lastPos += 4;

        result.put("isReported", res.substring(lastPos, lastPos + 1));
        lastPos += 1;

        result.put("fiscalCheckNumber", res.substring(lastPos, lastPos + 12));
        lastPos += 12;

        result.put("lastCheckNumber", res.substring(lastPos, lastPos + 12));
        lastPos += 12;

        result.put("lastSuccessCommand", res.substring(lastPos, lastPos + 4));
        lastPos += 4;

        result.put("versionId", res.substring(lastPos, lastPos + 4));
        lastPos += 4;

        result.put("versionDate", res.substring(lastPos, lastPos + 8));
        lastPos += 8;

        result.put("currentHeader", res.substring(lastPos, lastPos + 18));
        lastPos += 18;

        result.put("setupCurrencyDate", res.substring(lastPos, lastPos + 8));
        lastPos += 8;

        result.put("decimal", res.substring(lastPos, lastPos + 1));
        lastPos += 1;

        result.put("currencyName", res.substring(lastPos, lastPos + 3));

        Log.d(DBG_TAG, "CONf res: " + res);
       /* for (HashMap.Entry<String, String> entry : result.entrySet())
        {
            Log.d(DBG_TAG, "HashMap key: "+ entry.getKey()+" value: "+entry.getValue());
            //Log.d(DBG_TAG, "HashMap result value: " + entry.getValue());
        }*/

        return result;
    }


    /**
     @return                             Возвращает время до блокировки ЭККА в часах (сбрасывается на значение '72 часа' после
     *                                   каждого успешного сеанса связи с сервером ДПА). Отрицательное значение означает, что ЭККА
     *                                   заблокирован. Значение 999999999 означает, что ЭККА не поддерживает блокировку (старые
     *                                   модели)
     *
     */

    @MethodInfo(idMetod = 48, Params = "")
    public int GetTimeToPendingLock () throws MariaException{

        int  retvalue = 9999;
        final int cmd = 27;
        final int LenOfCmd = 1;
        String A = exchangeWithTaxService(String.format("%02d%02d",cmd,LenOfCmd));
        if (!A.contains(CMD_MDMD))return -1;
        try {
        A = A.replaceFirst(CMD_MDMD, "");
        int TypeMsg = Integer.parseInt(A.substring(0,2));
        int LenMsg = Integer.parseInt(A.substring(2,4));
       // Log.d(DBG_TAG, "GetTimeToPendingLock TypeMsg: " + TypeMsg+" LenMsg:"+LenMsg+" A="+A.length()+">["+A+"]");

            if ((TypeMsg == 47) && (LenMsg >= 32)) {

                int startindex = 88;
                int[] b = new int[]
                        {

                               /* Integer.parseInt(A.substring(2 + 2 + 42 * 2 + 2 * 0, 2 + 2 + 42 * 2 + 2 * 0 + 2),16),
                                Integer.parseInt(A.substring(2 + 2 + 42 * 2 + 2 * 1, 2 + 2 + 42 * 2 + 2 * 1 + 2),16),
                                Integer.parseInt(A.substring(2 + 2 + 42 * 2 + 2 * 2, 2 + 2 + 42 * 2 + 2 * 2 + 2),16),
                                Integer.parseInt(A.substring(2 + 2 + 42 * 2 + 2 * 3, 2 + 2 + 42 * 2 + 2 * 3 + 2),16)
                                */
                                Integer.parseInt(A.substring(startindex,   startindex + 2),16),
                                Integer.parseInt(A.substring(startindex+2, startindex + 4),16),
                                Integer.parseInt(A.substring(startindex+4, startindex + 6),16),
                                Integer.parseInt(A.substring(startindex+6, startindex + 8),16)
                        };
               retvalue =  (int)((int)(b[3]<<24)|(int)(b[2]<<16)|(int)(b[1]<<8)|(int)(b[0]));
                Log.d(DBG_TAG, "GetTimeToPendingLock res: " + retvalue );
               retvalue = retvalue/60/60;

            }
        }catch(Exception e){
            Log.e(DBG_TAG, "GetTimeToPendingLock Exception: " + e.getMessage());
        }

        return retvalue;
    }



    /**
     * Запрос количества пакетов данных, ожидающих
     * отправку на сервер эквайера
     *
     * @return                        возвращает HashMap<String, Integer>:
     *                                - Ключ: TOTAL_PKG, значение: Всего документов к отправке
     *                                - Ключ: TOTAL_CHK, значение: Количество чеков, ожидающих отправку
     *                                - Ключ: TOTAL_SDOC, значение: Количество служ. документов, ожидающих отправку
     *                                - Ключ: TOTAL_ZRREP, значение: Количество отчетов, ожидающих отправку
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     */
    @MethodInfo(idMetod = 119, Params = "")
    public HashMap<String,Integer> GetCountPendingDocs () throws MariaException{

        int [] Values = new int[]{0,0,0,0};

        final int cmd = 27;
        final int LenOfCmd = 1;
        String A = exchangeWithTaxService(String.format("%02d%02d",cmd,LenOfCmd));

        HashMap<String , Integer> result = new HashMap<>();
        result.put("TOTAL_PKG", -1);
        result.put("TOTAL_CHK", -1);
        result.put("TOTAL_SDOC", -1);
        result.put("TOTAL_ZRREP", -1);

        if (!A.contains(CMD_MDMD))return result;


        try {
            A = A.replaceFirst(CMD_MDMD, "");
            int TypeMsg = Integer.parseInt(A.substring(0,2));
            int LenMsg = Integer.parseInt(A.substring(2,4));
            // Log.d(DBG_TAG, "GetTimeToPendingLock TypeMsg: " + TypeMsg+" LenMsg:"+LenMsg+" A="+A.length()+">["+A+"]");

            if ((TypeMsg == 47) && (LenMsg >= 32)) {
                int startindex = 4;

                for (int i = 0;i<Values.length;i++){
                    int[] b = new int[]
                            {
                                    Integer.parseInt(A.substring(startindex,   startindex + 2),16),
                                    Integer.parseInt(A.substring(startindex+2, startindex + 4),16),
                                    Integer.parseInt(A.substring(startindex+4, startindex + 6),16),
                                    Integer.parseInt(A.substring(startindex+6, startindex + 8),16)
                            };
                    Values[i] =  (int)((int)(b[3]<<24)|(int)(b[2]<<16)|(int)(b[1]<<8)|(int)(b[0]));
                    //Log.d(DBG_TAG, "Values["+i+"] " + Values[i] );
                    startindex+=8;
                }
            }
        }catch(Exception e){
            Log.e(DBG_TAG, "GetCountPendingDocs Exception: " + e.getMessage());
        }
        result.clear();
        result.put("TOTAL_PKG",  Values[0]);
        result.put("TOTAL_CHK",  Values[1]);
        result.put("TOTAL_SDOC",  Values[2]);
        result.put("TOTAL_ZRREP",  Values[3]);
        return result;
    }



        /**
         * Обмен служебными данными с СПИ.
         *
         * @param  hexStringData          String от 2-х до 240 шестнадцатиричных символов
         *                                (от 1 до 120 пар символов) данные от ВУ для СПИ;
         *
         * @return                        String от 2-х до 240 шестнадцатиричных символов
         *                                (от 1 до 120 пар символов) данные от СПИ для ВУ.
         *
         *                                Пробрасывает исключение MariaException в случае неудачи.
         */
   // @MethodInfo(idMetod = 49, Params = {"hexStringData"})
    private String exchangeWithTaxService(String hexStringData) throws MariaException{
        String strData = CMD_MDMD + hexStringData;
        String res = sendCommand(strData);
        return res;
    }


    /**
     * Проверка состояния канала связи «Внешнее Устройство - ЭККР».
     *
     * @param  randomData             String до 252 произвольных символов;
     *
     * @return                        String копия randomData
     *
     *                                пробрасывает исключение MariaException в случае неудачи
     *
     */
    @MethodInfo(idMetod = 79, Params = {"randomData"})
    public String checkConnection(String randomData) throws MariaException{
        return sendCommand("SYNC" + randomData).replaceFirst("SYNC", "");
    }

    /********************************************************************************************************
     *                АВАНСОВЫЕ ПЛАТЕЖИ, БОНУСЫ, ПОДАРОЧНЫЕ СЕРТИФИКАТЫ
     *******************************************************************************************************/

    /**
     * Прием платежа на бонусный счет/подарочный сертификат/клубную карту
     *
     * @param  summOp                 сумма
     * @param  TypeOp                 1/2/3  бонусный счет/подарочный сертификат/клубная карта
     * @param  DescribeOp             128 символов дополнительная информация
     * @param  Tax1                    1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param  Tax2                    2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                Пробрасывает исключение MariaException в случае неудачи.
     */
    @MethodInfo(idMetod = 95, Params = {"summOp","TypeOp","DescribeOp","Tax1","Tax2"})
    public int PutAccountingPayments (double summOp, int TypeOp, String DescribeOp, int Tax1, int Tax2) throws MariaException {
        if ((!isConvertableToNumeric(summOp))||
                (!isConvertableToNumeric(TypeOp))||
                (!isConvertableToNumeric(Tax1))||
                (!isConvertableToNumeric(Tax2)))
        {
            return 1;
        }

        int result = 0;

        if (!Check.isOpenCheck())return 1;

        String CmdStr ="";
        switch (TypeOp){
            case 1:
                CmdStr = CMD_BONS;//Прием платежа на бонусный счет
                break;
            case 2:
                CmdStr = CMD_SERT;//Прием платежа за подарочный сертификат
                break;
            case 3:
                CmdStr = CMD_CLCR;//Прием платежа на клубную карту
                break;
            default:
                return 0;
        }
        String StrParams = Check.AccountingOfPayments(summOp, TypeOp, DescribeOp,Tax1,Tax2, 0);
        if (StrParams.length()==0) return 0;

        return  ((sendCommand(CmdStr+StrParams).trim().contains("FISC"))?0:1);
    }

    /**
     * Списание средств с бонусного счета/подарочного сертификата/клубной карты
     *
     * @param  summOp                 сумма
     * @param  TypeOp                 1/2/3/4  бонусный счет/подарочный сертификат/клубная карта/Скидка на общий оборот
     * @param  DescribeOp             128 символов дополнительная информация
     * @param  Tax1                    1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param  Tax2                    2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     *
     * @return                        возвращает 0 в случае успеха, пробрасывает исключение
     *                                Пробрасывает исключение MariaException в случае неудачи.
     */
    @MethodInfo(idMetod = 96, Params = {"summOp","TypeOp","DescribeOp","Tax1","Tax2"})
    public int TakeAccountingPayments (double summOp, int TypeOp, String DescribeOp, int Tax1, int Tax2) throws MariaException {

        if (!Check.isOpenCheck())return 1;
        if ((!isConvertableToNumeric(summOp))||
                (!isConvertableToNumeric(TypeOp))||
                (!isConvertableToNumeric(Tax1))||
                (!isConvertableToNumeric(Tax2)))
        {
            return 2;
        }
        int result = 0;

        double summaOperation =0;
        try {
            DecimalFormat twoD = new DecimalFormat("#.##");
            summaOperation = (double) (Double.valueOf(twoD.format(summOp).replace(",", ".")));

        }catch (Exception e){
            return 1;
        }

        String CmdStr ="";
        switch (TypeOp){
            case 1:
                CmdStr = CMD_mBNS;//Учет платежей с бонусного счета (уменьшение оборота  реализации-возврата)
                break;
            case 2:
                CmdStr = CMD_mSRT;//Учет платежа за подарочный сертификат (уменьшение оборота реализации-возврата)
                break;
            case 3:
                CmdStr = CMD_mCLC;//Учет платежей с клубной карты (уменьшение оборота реализации-возврата)
                break;
            case 4:
                CmdStr = CMD_mDIS ;//Скидка на общий оборот в разрезе схем налогообложения. (уменьшение оборота реализации-возврата)
                break;

        }
        String StrParams = Check.AccountingOfPayments(summaOperation, TypeOp, DescribeOp,Tax1,Tax2, 1);
        if (StrParams.length()==0) return 1;

        return  ((sendCommand(CmdStr+StrParams).trim().contains("FISC"))?0:1);
    }

    /*
    * Открытие чека “заказа”. Прием авансового платежа.
    *
    *
    * @param departmentName    до 15-ти символов идентификатор (наименование) торгового отдела.
    *
    * @return                   0/1 успех/ошибка
     */
    @MethodInfo(idMetod = 97, Params = {"departmentName"})
    public int OpenOrderCheck  (String departmentName) throws MariaException {
        int result = 1;
        sendCommandInner(CMD_CANC);
        String tmpRes = sendCommand(CMD_PRAV + departmentName);
        if (tmpRes.trim().contains(ANSW_READY)) {
            Check.OpenOrderCheck();
            result = 0;
        }
        return result;
    }

    /*
    * Печать описания одной заказываемой позиции товара(услуги) в пределах чека заказа.
    *
    * @param <nameTov>          до 32 символов наименование (услуги)
    * @param <count>            количество единиц
    * @param <singleCost>       стоимость единицы товара (услуги)
    * @param <isDivisible>      символ признак делимости, где
                                '0' - делимый, при этом значение <п2> интерпретируется как xxx,xxx.
                                '1' - неделимый, при этом значение <п2> интерпретируется как xxxxxx.
    * @return                   0/1/2-3 успех/ошибка протокола/ошибки параметров функции
    * */
    @MethodInfo(idMetod = 98, Params = {"nameTov", "count", "singleCost", "isDivisible"})
    public int OrderLine(String nameTov, double count, double Cost, int isDivisible) throws MariaException{

        if ((!isConvertableToNumeric(count))||
                (!isConvertableToNumeric(Cost))||
                (!isConvertableToNumeric(isDivisible))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 2;
        }

        if (!Check.isOpenCheck())return 1;

        if (isDivisible==1)//неделимый товар
        {
            if (((double)count-(int)count)>0)return 3;
        }

        try {
            String strData = Check.CreateOrderLine(nameTov, count, Cost, isDivisible);
            String res = sendCommand(CMD_AVLS+ strData);
            return (res.contains(ANSW_READY)?0:1);
        }catch(Exception e){
            Log.e(DBG_TAG, "AdvanceLine Exception: " + e.getMessage());
        }
        return 1;
    }

    /**
     * Регистрация оборота приема аванса и закрытие чека заказа.
     * <br>
     * <br>
     *  @param NameOrder    до 128 символов дополнительная информация об авансе
     *  @param Tax1           1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     *  @param Tax2           2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     *
     * @return                   0/1/2 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 99, Params = {"NameOrder", "Tax1", "Tax2"})
    public  int CloseOrder(String NameOrder, int Tax1, int Tax2 )throws MariaException{
        if (!Check.isOpenCheck())return 1;

        if (    (!isConvertableToNumeric(Tax1))||
                (!isConvertableToNumeric(Tax2))){
            return 2;
        }

        int result = 1;

        String tmpStr = CMD_AVNS+Check.CreateOrderTakePay(NameOrder,Check.CloseCheck.get_sumProd(),Tax1,Tax2);
        if (sendCommand(tmpStr).contains(CMD_FISC)){
           if(sendCommand(CMD_COMP+Check.getStringCOMP()).contains(CMD_COMP))
             result = 0;
        }
        return result;
    }


    /*
     * Открытие чека “выполнения заказа”. Расчет по авансовому платежу.
     *
     *
     * @param departmentName   до 15-ти символов идентификатор (наименование) торгового отдела.
     *
     * @return                   0/1 успех/ошибка
     */
    @MethodInfo(idMetod = 100, Params = {"departmentName"})
    public int OpenAdvancePaymentCheck  (String departmentName) throws MariaException {
        int result = 1;
        sendCommandInner(CMD_CANC);

        String tmpRes = sendCommand(CMD_CLAV + departmentName);
        if (tmpRes.trim().contains(ANSW_READY)) {
            Check.OpenCheck(false);
            result = 0;
        }
        return result;
    }



   /*
    * Учет авансового платежа (уменьшение оборота реализации).
    *
    * @param <sum>               сумма учитываемого аванса
    * @param <Tax1>              1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
    * @param <Tax2>              2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
    * @param <ExtendedInfo>      до 128 символов дополнительная информация об авансе,

    * @return                   0/1/2-3 успех/ошибка протокола/ошибки параметров функции
    * */
    @MethodInfo(idMetod = 101, Params = {"sum", "Tax1", "Tax2", "ExtendedInfo"})
    public int AdvancePaymentLine(double sum, int Tax1, int Tax2, String ExtendedInfo) throws MariaException{

        if ((!isConvertableToNumeric(sum))||
                (!isConvertableToNumeric(Tax1))||
                (!isConvertableToNumeric(Tax2))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 2;
        }

        if (!Check.isOpenCheck())return 1;

        try {
            String strData = Check.CreateAdvancePaymentLine(sum, Tax1, Tax2, ExtendedInfo);
            String res = sendCommand(CMD_mAVN+ strData);
            return (res.contains(CMD_FISC)?0:1);
        }catch(Exception e){
            Log.e(DBG_TAG, "AdvancePaymentLine Exception: " + e.getMessage());
        }
        return 1;
    }


    /******************************************************************************************************************
     *                                  возврат аванса
     ******************************************************************************************************************/

    /*
     * Открытие чека “возврата аванса”. Расчет по авансовому платежу.
     *
     *@param departmentName   до 15-ти символов идентификатор (наименование) торгового отдела.
     *@return                   0/1 успех/ошибка
     */
    @MethodInfo(idMetod = 106, Params = {"departmentName"})
    public int OpenReturnAdvance(String departmentName) throws MariaException{
        int result = 1;
        sendCommandInner(CMD_CANC);

        String tmpRes = sendCommand(CMD_PRBA + departmentName);
        if (tmpRes.trim().contains(ANSW_READY)) {
            Check.OpenCheck(false);
            result = 0;
        }
        return result;
    }

    /*
     * Регистрация оборота возврата полной суммы ранее принятого аванса.
     *
     * @param <sum>               сумма возвращаемого аванса
     * @param <Tax1>              1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param <Tax2>              2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param <ExtendedInfo>      до 128 символов дополнительная информация об авансе,

     * @return                   0/1/2-3 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 107, Params = {"sum", "Tax1", "Tax2", "ExtendedInfo"})
    public int RegistrationReturnAcceptedAdvance(double sum, int Tax1, int Tax2, String ExtendedInfo) throws MariaException{

        if ((!isConvertableToNumeric(sum))||
                (!isConvertableToNumeric(Tax1))||
                (!isConvertableToNumeric(Tax2))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 2;
        }
        if (!Check.isOpenCheck())return 1;

        try {
            String strData = Check.CreateReturnAdvancePaymentLine(sum, Tax1, Tax2, ExtendedInfo);
            return (sendCommand(CMD_BAAV+ strData).contains(CMD_FISC)?0:1);
        }catch(Exception e){
            Log.e(DBG_TAG, "RegistrationReturnAcceptedAdvance Exception: " + e.getMessage());
        }
        return 1;
    }


    /*
     * Учет сумм отпущенных товаров-услуг при возврате аванса.
     *
     * @param <sum>               сумма возвращаемого аванса
     * @param <Tax1>              1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param <Tax2>              2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param <ExtendedInfo>      до 128 символов дополнительная информация об авансе,

     * @return                   0/1/2-3 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 108, Params = {"sum", "Tax1", "Tax2", "ExtendedInfo"})
    public int RegistrationSummByAcceptedAdvance(double sum, int Tax1, int Tax2, String ExtendedInfo) throws MariaException{

        if ((!isConvertableToNumeric(sum))||
                (!isConvertableToNumeric(Tax1))||
                (!isConvertableToNumeric(Tax2))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 2;
        }
        if (!Check.isOpenCheck())return 1;

        try {
            String strData = Check.CreateReturnAdvancePaymentLine(sum*(-1), Tax1, Tax2, ExtendedInfo);
            return (sendCommand(CMD_BAAR+ strData).contains(CMD_FISC)?0:1);
        }catch(Exception e){
            Log.e(DBG_TAG, "RegistrationSummByAcceptedAdvance Exception: " + e.getMessage());
        }
        return 1;
    }

    /******************************************************************************************************************
     *                                  Команды ЖД билетов
     ******************************************************************************************************************/

    /*
     * Открытие чека продажи билета (багажной китанции).
     *
     * @param TicketOrBaggage            0/1 билет/багажная квитанция
     * @param TypeTicket                [0..4] «ПОЛНЫЙ», «ДЕТСКИЙ», «ЛЬГОТНЫЙ», «АБОНЕМЕНТНЫЙ», «БЕСПЛАТНЫЙ»
     *                                  Если TypeTicket из ряда {«ДЕТСКИЙ», «ЛЬГОТНЫЙ», «БЕСПЛАТНЫЙ»}-
     *                                  нижеследующие параметры являются обязательными:
     * @param SysNumberDoc              0 до 20 символов Системный номер документа (билета/квитанции)
     * @param IdentDocExemption         0 до 40 символов Идентификация документа на право на льготы
     * @param RateExemption             0 до 10 символов Идентификатор льготной ставки
     *
     * @return                          0/1/2-3 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 109, Params = {"TicketOrBaggage", "TypeTicket", "SysNumberDoc", "IdentDocExemption", "RateExemption"})
    public int OpenTicketForSale(int TicketOrBaggage, int TypeTicket, String SysNumberDoc, String IdentDocExemption, String RateExemption) throws MariaException {
        if ((!isConvertableToNumeric(TicketOrBaggage))||
                (!isConvertableToNumeric(TypeTicket))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 2;
        }
        if(SysNumberDoc.trim().length()>20) return 2;
        if(IdentDocExemption.trim().length()>40) return 2;
        if(RateExemption.trim().length()>10) return 2;

        if((TicketOrBaggage!=0)&&(TicketOrBaggage!=1)) return 2;
        if((TypeTicket<0)&&(TypeTicket>4)) return 2;
        int directionFiscalOperation = 0;
        String StrSend = CMD_TKTP;
        StrSend += String.format("%d%d%d",TicketOrBaggage, TypeTicket,directionFiscalOperation);
        StrSend += String.format("%02d%s",SysNumberDoc.trim().length(),SysNumberDoc.trim() );
        StrSend += String.format("%02d%s",IdentDocExemption.trim().length(),IdentDocExemption.trim() );
        StrSend += String.format("%02d%s",RateExemption.trim().length(),RateExemption.trim() );
        return (sendCommand(StrSend).contains(ANSW_READY)?0:1);
    }
    /*
     * Открытие чека возврата билета (багажной китанции).
     *
     * @param TicketOrBaggage            0/1 билет/багажная квитанция
     * @param TypeOperation              1/2 «возврат билета» /«возврат как погашение испорченного документа»
     * @param TypeTicket                [0..4] «ПОЛНЫЙ», «ДЕТСКИЙ», «ЛЬГОТНЫЙ», «АБОНЕМЕНТНЫЙ», «БЕСПЛАТНЫЙ»
     *                                  Если TypeTicket из ряда {«ДЕТСКИЙ», «ЛЬГОТНЫЙ», «БЕСПЛАТНЫЙ»}-
     *                                  нижеследующие параметры являются обязательными:
     * @param SysNumberDoc              0 до 20 символов Системный номер документа (билета/квитанции)
     * @param IdentDocExemption         0 до 40 символов Идентификация документа на право на льготы
     * @param RateExemption             0 до 10 символов Идентификатор льготной ставки
     *
     * @return                          0/1/2 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 110, Params = {"TicketOrBaggage", "TypeOperation", "TypeTicket", "SysNumberDoc", "IdentDocExemption", "RateExemption"})
    public int OpenTicketForReturn(int TicketOrBaggage, int TypeOperation, int TypeTicket, String SysNumberDoc, String IdentDocExemption, String RateExemption) throws MariaException {
        if ((!isConvertableToNumeric(TicketOrBaggage))||
                (!isConvertableToNumeric(TypeTicket))||
                (!isConvertableToNumeric(TypeOperation))) {
            Log.e(DBG_TAG, "some parameters is not numeric");
            return 2;
        }
        if(SysNumberDoc.trim().length()>20) return 2;
        if(IdentDocExemption.trim().length()>40) return 2;
        if(RateExemption.trim().length()>10) return 2;
        if((TypeOperation<1)&&(TypeOperation>2)) return 2;

        if((TicketOrBaggage!=0)&&(TicketOrBaggage!=1)) return 2;
        if((TypeTicket<0)&&(TypeTicket>4)) return 2;

        String StrSend = CMD_TKTP;
        StrSend += String.format("%d%d%d",TicketOrBaggage, TypeTicket,TypeOperation);
        StrSend += String.format("%02d%s",SysNumberDoc.trim().length(),SysNumberDoc.trim() );
        StrSend += String.format("%02d%s",IdentDocExemption.trim().length(),IdentDocExemption.trim() );
        StrSend += String.format("%02d%s",RateExemption.trim().length(),RateExemption.trim() );
        return (sendCommand(StrSend).contains(ANSW_READY)?0:1);
    }

    /*
     * Ввод описания маршрута
     *
     * @param NameStationDeparture      от 1 до 99 символов Наименование станции отправления
     * @param NameStationDestination    от 0 до 40 символов Наименование станции назначения
     * @param NameNodePoint             от 0 до 40 символов Наименование узлового пункта.
     * @param NameTransplantPoint       от 0 до 11 символов Наименование пункта пересадки
     * @param DateExpired               10 символов граничная дата действия документа
     *
     * @return                          0/1/2 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 111, Params = {"NameStationDeparture", "NameStationDestination", "NameNodePoint", "NameTransplantPoint", "DateExpired"})
    public int TicketRouteDescriptions(String NameStationDeparture,String NameStationDestination,String NameNodePoint,String NameTransplantPoint,String DateExpired) throws MariaException {
        if(NameStationDeparture.trim().length()>99) return 2;
        if(NameStationDestination.trim().length()>40) return 2;
        if(NameNodePoint.trim().length()>40) return 2;
        if(NameTransplantPoint.trim().length()>11) return 2;
        if(DateExpired.trim().length()>10) return 2;
        String StrSend = CMD_TKRC;

        StrSend += String.format("%02d%s",NameStationDeparture.trim().length(),NameStationDeparture.trim());
        StrSend += String.format("%02d%s",NameStationDestination.trim().length(),NameStationDestination.trim());
        StrSend += String.format("%02d%s",NameNodePoint.trim().length(),NameNodePoint.trim());
        StrSend += String.format("%02d%s",NameTransplantPoint.trim().length(),NameTransplantPoint.trim());
        StrSend += String.format("%s",DateExpired.trim());
        return (sendCommand(StrSend).contains(ANSW_READY)?0:1);
    }


    /*
     * Ввод сумм составляющих стоимости билета (квитанции)
     *
     *                                  составляющая билета «ТАРИФ» «БАГАЖ»
     * @param CntPlaceOrPsngrs        (числовое значение от 1 до 9999) количество пассажирских мест или мест багажа
     * @param CostPlaceOrPsngrs         стоимость одного места (стоимость составляющей «ТАРИФ» или «БАГАЖ»)
     * @param TaxSchemePlaceOrPsngrs    0/1 применение налогов по обычной схеме / по приоритетной схеме
     * @param Tax1PlaceOrPsngrs         1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param Tax2PlaceOrPsngrs         2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     *
     *                                  составляющая билета «СТРАХОВОЙ ПЛАТЕЖ»
     * @param CntInsurancePayment       (числовое значение от 1 до 9999) количество единиц составляющей стоимости «СТРАХОВОЙ ПЛАТЕЖ»
     * @param CostInsurancePayment      стоимость составляющей стоимости «СТРАХОВОЙ ПЛАТЕЖ»
     * @param TaxSchemeInsurance        0/1 применение налогов по обычной схеме / по приоритетной схеме
     * @param Tax1Insurance             1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param Tax2Insurance             2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается

                                        составляющая билета  «КОМИССИОННЫЙ СБОР»
     * @param CntCommissionFee         (числовое значение от 1 до 9999) ) количество единиц составляющей стоимости «КОМИССИОННЫЙ СБОР»
     * @param CostCommissionFee        стоимость составляющей стоимости «КОМИССИОННЫЙ СБОР»
     * @param TaxSchemeCommission      0/1 применение налогов по обычной схеме / по приоритетной схеме
     * @param Tax1Commission           1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param Tax2Commission        2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается

     * @return                          0/1/2 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 112, Params = {"CntPlaceOrPsngrs", "CostPlaceOrPsngrs", "TaxSchemePlaceOrPsngrs", "Tax1PlaceOrPsngrs", "Tax2PlaceOrPsngrs",
                                         "CntInsurancePayment", "CostInsurancePayment", "TaxSchemeInsurance", "Tax1Insurance", "Tax2Insurance",
                                         "CntCommissionFee", "CostCommissionFee", "TaxSchemeCommission", "Tax1Commission", "Tax2Commission" })
    public int SumComponentsOfTicket(int CntPlaceOrPsngrs, double CostPlaceOrPsngrs, int TaxSchemePlaceOrPsngrs, int Tax1PlaceOrPsngrs, int Tax2PlaceOrPsngrs,
                                      int CntInsurancePayment , double CostInsurancePayment, int TaxSchemeInsurance, int Tax1Insurance, int Tax2Insurance,
                                      int CntCommissionFee, double CostCommissionFee, int TaxSchemeCommission, int Tax1Commission, int Tax2Commission ) throws MariaException {

        Log.d(DBG_TAG, "SumComponentsOfTicket");
        if( (!isConvertableToNumeric(CntPlaceOrPsngrs))||
            (!isConvertableToNumeric(CostPlaceOrPsngrs))||
            (!isConvertableToNumeric(TaxSchemePlaceOrPsngrs))||
            (!isConvertableToNumeric(Tax1PlaceOrPsngrs))||
            (!isConvertableToNumeric(Tax2PlaceOrPsngrs))||
            (!isConvertableToNumeric(CntInsurancePayment))||
            (!isConvertableToNumeric(CostInsurancePayment))||
            (!isConvertableToNumeric(TaxSchemeInsurance))||
            (!isConvertableToNumeric(Tax1Insurance))||
            (!isConvertableToNumeric(Tax2Insurance))||
            (!isConvertableToNumeric(CntCommissionFee))||
            (!isConvertableToNumeric(CostCommissionFee))||
            (!isConvertableToNumeric(TaxSchemeCommission))||
            (!isConvertableToNumeric(Tax1Commission))||
            (!isConvertableToNumeric(Tax2Commission))) return 2;

        if ((TaxSchemePlaceOrPsngrs<0)&&(TaxSchemePlaceOrPsngrs>1)) return 2;
        if ((TaxSchemeInsurance<0)&&(TaxSchemeInsurance>1)) return 2;
        if ((TaxSchemeCommission<0)&&(TaxSchemeCommission>1)) return 2;

        DecimalFormat twoD = new DecimalFormat("#.##");

        String StrSend="";
        try {
            StrSend = CMD_TKSM;
            //составляющая билета «ТАРИФ» «БАГАЖ»
            double _Cost = (double) (Double.valueOf(twoD.format(CostPlaceOrPsngrs).replace(",", ".")));

            StrSend += String.format("%04d%09d%d%s%s", CntPlaceOrPsngrs, (int)(_Cost * 100), TaxSchemePlaceOrPsngrs,
                    (Tax1PlaceOrPsngrs > 0) ? iMariaCommands.TaxName[Tax1PlaceOrPsngrs - 1] : "0",
                    (Tax2PlaceOrPsngrs > 0) ? iMariaCommands.TaxName[Tax2PlaceOrPsngrs - 1] : "0");


            //составляющая билета «СТРАХОВОЙ ПЛАТЕЖ»
            _Cost = (double) (Double.valueOf(twoD.format(CostInsurancePayment).replace(",", ".")));
            StrSend += String.format("%04d%09d%d%s%s", CntInsurancePayment,(int)(_Cost * 100), TaxSchemeInsurance,
                    (Tax1Insurance > 0) ? iMariaCommands.TaxName[Tax1Insurance - 1] : "0",
                    (Tax2Insurance > 0) ? iMariaCommands.TaxName[Tax2Insurance - 1] : "0");
            //составляющая билета  «КОМИССИОННЫЙ СБОР»

            _Cost = (double) (Double.valueOf(twoD.format(CostCommissionFee).replace(",", ".")));
            StrSend += String.format("%04d%09d%d%s%s", CntCommissionFee, (int)(_Cost * 100), TaxSchemeCommission,
                    (Tax1Commission > 0) ? iMariaCommands.TaxName[Tax1Commission - 1] : "0",
                    (Tax2Commission > 0) ? iMariaCommands.TaxName[Tax2Commission - 1] : "0");

        }catch (Exception e){
            Log.e(DBG_TAG, "Exception SumComponentsOfTicket "+e);
        }

        return (sendCommand(StrSend).contains(CMD_FISC)?0:1);
    }


    /*
     * Ввод суммы  удержания по билету
     *
     * @param SumRetention               сумма удержания
     * @param TaxScheme                 0/1 применение налогов по обычной схеме / по приоритетной схеме
     * @param Tax1                      1 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param Tax2                      2 идентификатор(номер) схемы налогообложения оборота   [1..8]['А'..'З']  0 - налог не учитывается
     * @param NameRetention             от 0 до 128 символов наименование удержания.
     *
     * @return                          0/1/2 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 113, Params = {"SumRetention", "TaxScheme", "Tax1", "Tax2", "NameRetention"})
    public int TicketRetention(double SumRetention,int TaxScheme,int Tax1,int Tax2,String NameRetention) throws MariaException {
        if( (!isConvertableToNumeric(SumRetention))||
                (!isConvertableToNumeric(TaxScheme))||
                (!isConvertableToNumeric(Tax1))||
                (!isConvertableToNumeric(Tax2))) return 2;

        if ((TaxScheme<0)&&(TaxScheme>1)) return 2;
        if ((Tax1<0)&&(Tax1>8)) return 2;
        if ((Tax2<0)&&(Tax1>8)) return 2;
        if (NameRetention.trim().length()>128) return 2;

        DecimalFormat twoD = new DecimalFormat("#.##");
        String StrSend = CMD_TKMS;

        double _SumRetention = (double)(Double.valueOf(twoD.format(SumRetention).replace(",",".")));
        StrSend += String.format("%09d%s%s%s%s",(int)(_SumRetention*100),
                            TaxScheme,
                        (Tax1>0)?iMariaCommands.TaxName[Tax1-1]:"0",
                        (Tax2>0)?iMariaCommands.TaxName[Tax2-1]:"0",
                        NameRetention.trim());
        return (sendCommand(StrSend).contains(ANSW_READY)?0:1);
    }

    /*
     * Закрытие чека билета  (багажной квитанции).
     *
     * @param FormOfPayment             код формы оплаты
     * @param SummPaymentCash           сумма полученная от клиента. Имеет значение только для чека продажи и формы оплаты наличные.
     * @param IdPaymentSystem           до 20-ти символов идентификатор транзакции платежной системы. Имеет смысл для указанных форм оплаты «не наличные»;
     *
     * @return                          0/1/2 успех/ошибка протокола/ошибки параметров функции
     */
    @MethodInfo(idMetod = 114, Params = {"FormOfPayment", "SummPaymentCash", "IdPaymentSystem"})
    public int TicketClose(int FormOfPayment,double SummPaymentCash,String IdPaymentSystem) throws MariaException {
        if( (!isConvertableToNumeric(FormOfPayment))||
                (!isConvertableToNumeric(SummPaymentCash))) return 2;

        if (IdPaymentSystem.trim().length()>20) return 2;

        DecimalFormat twoD = new DecimalFormat("#.##");

        String StrSend = CMD_TKCL;

        double _SummPaymentCash = (double)(Double.valueOf(twoD.format(SummPaymentCash).replace(",",".")));
        StrSend += String.format("%d%010d%s",FormOfPayment,(int)( _SummPaymentCash*100),IdPaymentSystem.trim());
        return (sendCommand(StrSend).contains(CMD_COMP)?0:1);

    }
    /******************************************************************************************************************
    *
    ******************************************************************************************************************/

    /*
    * Получение состояния аккумулятора
    *
    * @return             (-1) ошибка, заряд аккумулятора в процентах
    * */

    @MethodInfo(idMetod = 117, Params = "")
    public int GetBatteryStatus() throws MariaException
    {
        int BatteryValue = -1;
        String res = sendCommand(CMD_VBAT);
        int indexValue = res.indexOf("chrg:");
        int indexPercent = res.indexOf("%");
        if ((indexValue>0)&&(indexPercent>0)){
            String StrValueBat = res.substring(indexValue+("chrg:").length()+1,indexPercent);
            if (isConvertableToNumeric(StrValueBat)) {
                try {
                    BatteryValue = Integer.valueOf(StrValueBat);
                }catch (Exception ex){
                    return (-1);
                }
            }else return (-1);
        }
        return BatteryValue;
    }


    /******************************************************************************************************************/
    /*
     * Ввод данных по слипу после успешной транзакции по платежному терминалу
     *
     * @param LenIdAcquirer,       //<1>   2 символа цифры (числовое значение от 0 до 99) наличие и длина поля  «ID эквайра, торгiвця»
     * @param IdAcquirer,          //<2>   от 1 до 99 символов в соответствии со значением <п1> «ID эквайра, торгiвця».
     * @param LenIdDevice,         //<3>   2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «ID пристрою»
     * @param IdDevice,            //<4>   от 1 до 32 символов в соответствии со значением <п3> «ID пристрою».
     * @param LenTypeOperation,    //<5>   2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «Вид операцiї»
     * @param TypeOperation,       //<6>   от 1 до 32 символов в соответствии со значением <п5> «Вид операцiї».
     * @param LenPaymentDevice,    //<п7>  2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «ЕПЗ»
     * @param PaymentDevice,       //<п8>  от 1 до 32 символов в соответствии со значением <п7> «ЕПЗ».
     * @param LenCodeAvtorization, //<п9>	2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «Код авт»
     * @param CodeAvtorization,    //<п10>	от 1 до 32 символов в соответствии со значением <п9> «Код авт».
     * @param SummKomission,       //<п11>	10 символов цифры сумма комиссии в копейках
     * @param CashierSignature,    //<п12>	1 символ «1» или «0» присутствует или нет место для подписи кассира.
     * @param OwnerSignature       //<п13>	1 символ «1» или «0» присутствует или нет место для подписи владельца «ЕПЗ».) throws MariaException{
     *
     * @return                          0/1/2 успех/ошибка протокола/ошибки параметров функции
     * */

    @MethodInfo(idMetod = 118, Params = {"LenIdAcquirer","IdAcquirer","LenIdDevice","IdDevice","LenTypeOperation","TypeOperation","LenPaymentDevice","PaymentDevice","LenCodeAvtorization","CodeAvtorization","SummKomission","CashierSignature","OwnerSignature"})
    public int ReceiptPaymantTerminal(
            int LenIdAcquirer,          //<1>   2 символа цифры (числовое значение от 0 до 99) наличие и длина поля  «ID эквайра, торгiвця»
            String IdAcquirer,          //<2>   от 1 до 99 символов в соответствии со значением <п1> «ID эквайра, торгiвця».
            int LenIdDevice,            //<3>   2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «ID пристрою»
            String IdDevice,            //<4>   от 1 до 32 символов в соответствии со значением <п3> «ID пристрою».
            int LenTypeOperation,       //<5>   2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «Вид операцiї»
            String TypeOperation,       //<6>   от 1 до 32 символов в соответствии со значением <п5> «Вид операцiї».
            int LenPaymentDevice,       //<п7>  2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «ЕПЗ»
            String PaymentDevice,       //<п8>  от 1 до 32 символов в соответствии со значением <п7> «ЕПЗ».
            int LenCodeAvtorization,    //<п9>	2 символа цифры (числовое значение от 0 до 32) наличие и длина поля  «Код авт»
            String CodeAvtorization,    //<п10>	от 1 до 32 символов в соответствии со значением <п9> «Код авт».
            double SummKomission,       //<п11>	10 символов цифры сумма комиссии в копейках
            boolean CashierSignature,   //<п12>	1 символ «1» или «0» присутствует или нет место для подписи кассира.
            boolean OwnerSignature      //<п13>	1 символ «1» или «0» присутствует или нет место для подписи владельца «ЕПЗ».) throws MariaException{
            )throws MariaException {

        final int LEN_MIN_IdAcquirer = 0;
        final int LEN_MAX_IdAcquirer = 99;
        final int LEN_MIN_IdDevice = 0;
        final int LEN_MAX_IdDevice = 32;
        final int LEN_MIN_TypeOperation = 0;
        final int LEN_MAX_TypeOperation = 32;
        final int LEN_MIN_PaymentDevice = 0;
        final int LEN_MAX_PaymentDevice = 32;
        final int LEN_MIN_CodeAvtorization = 0;
        final int LEN_MAX_CodeAvtorization = 32;

        if ((!isConvertableToNumeric(LenIdAcquirer)) ||
           (!isConvertableToNumeric(LenIdDevice)) ||
           (!isConvertableToNumeric(LenTypeOperation)) ||
           (!isConvertableToNumeric(LenPaymentDevice)) ||
           (!isConvertableToNumeric(LenCodeAvtorization)) ||
           (!isConvertableToNumeric(SummKomission))) return 2;

        if ((LenIdAcquirer<LEN_MIN_IdAcquirer)&&(LenIdAcquirer>LEN_MAX_IdAcquirer))return 2;
        if ((LenIdDevice<LEN_MIN_IdDevice)&&(LenIdDevice>LEN_MAX_IdDevice))return 2;
        if ((LenTypeOperation<LEN_MIN_TypeOperation)&&(LenTypeOperation>LEN_MAX_TypeOperation))return 2;
        if ((LenPaymentDevice<LEN_MIN_PaymentDevice)&&(LenPaymentDevice>LEN_MAX_PaymentDevice))return 2;
        if ((LenCodeAvtorization<LEN_MIN_CodeAvtorization)&&(LenCodeAvtorization>LEN_MAX_CodeAvtorization))return 2;

        if (IdAcquirer.trim().length()>LEN_MAX_IdAcquirer)return 2;
        if (IdDevice.trim().length()>LEN_MAX_IdDevice)return 2;
        if (TypeOperation.trim().length()>LEN_MAX_TypeOperation)return 2;
        if (PaymentDevice.trim().length()>LEN_MAX_PaymentDevice)return 2;
        if (CodeAvtorization.trim().length()>LEN_MAX_CodeAvtorization)return 2;

        String StrSend = CMD_PSDT;
        DecimalFormat twoD = new DecimalFormat("#.##");

        double _SummKomission = (double)(Double.valueOf(twoD.format(SummKomission).replace(",",".")));
        StrSend += String.format("%02d%s%02d%s%02d%s%02d%s%02d%s%010d%d%d",
                LenIdAcquirer,
                IdAcquirer.trim(),
                LenIdDevice,
                IdDevice.trim(),
                LenTypeOperation,
                TypeOperation.trim(),
                LenPaymentDevice,
                PaymentDevice.trim(),
                LenCodeAvtorization,
                CodeAvtorization.trim(),
                (int)(_SummKomission*100),
                (CashierSignature==true)?1:0,
                (OwnerSignature==true)?1:0);
        return (sendCommand(StrSend).contains(ANSW_READY)?0:1);
    }
    /*****************************************************************************************************************/

    /*
     * Принудительно передать данные на эквайер
     *
     * @return                   0/1 успех/ошибка
     */
    @MethodInfo(idMetod = 102, Params = {""})
    public int TransferForTaxPurposes  () throws MariaException {
        return(sendCommand(CMD_SPKF).trim().contains(ANSW_READY)?0:1);

    }

    //@MethodInfo(idMetod = 81, Params = "")
    private String sendFakeDPIRequest() throws MariaException
    {
        String res = sendCommand("MDMD2301BB");
        return res.replaceFirst("MDMD", "");
    }
    private void switchCRCcheckMode(int mode) throws MariaException{
        sendCommand("CSIN" + mode);
    }

    private boolean isConvertableToNumeric(Object val){
        String regex = "-?\\d+(\\.\\d+)?";
        return ((val+"").matches(regex));

    }



}

