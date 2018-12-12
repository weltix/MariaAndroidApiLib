package com.resonance.ekka.mariaapilib;

import android.util.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;

public class ClassCheck implements iMariaCommands {

    private static boolean bOpenCheck = false;
    private static boolean bReturnOperation = false;
    private classFiscLine FiscLine = null;
    private classOrder OrderLine = null;
    public classCloseCheck CloseCheck=null;

    public ClassCheck() {

    }

    public void OpenCheck(boolean isReturn) {
        Log.w(DBG_TAG, "OpenCheck ");
        bReturnOperation = isReturn;//открытие чека для возврата
        bOpenCheck = true;

        try {
            CloseCheck = new classCloseCheck();
            FiscLine = new classFiscLine();
        }catch (Exception e){
            Log.e(DBG_TAG, "OpenCheck Exception "+e.getMessage());
        };
    }

    public void CloseCheck() {
        Log.w(DBG_TAG, "CloseCheck ");
        bOpenCheck = false;
        FiscLine = null;
        OrderLine = null;
        CloseCheck = null;
    }

    public boolean isOpenCheck() {
        return bOpenCheck;
    }

    public void SetReturnOperation() {
        bReturnOperation = true;
    }

    public boolean isReturnOperation() {
        return bReturnOperation;
    }

    public String CreateFiscLine(String nameTov, double count, double singleCost, int isDivisible, int tax1, int tax2, int goodsNumber,int isDiscountOrExtraCharge, String nameDiscount, double discountOrExtraChargeSum){
        DecimalFormat twoD = new DecimalFormat("#.##");
        double _count = (double)((isDivisible==0)?count:((int)count));

        double sumProd = (double)(Double.valueOf(twoD.format(_count * singleCost).replace(",",".")));
       Log.w(DBG_TAG, "sumProd "+sumProd);
        double sumDiscountOrCharge = (double) (Double.valueOf(twoD.format(discountOrExtraChargeSum).replace(",", ".")));

        if(isDiscountOrExtraCharge==0)//нет скидки
            sumDiscountOrCharge=0;
        else if(isDiscountOrExtraCharge==1)//скидка
            sumDiscountOrCharge*=(-1);

        Log.w(DBG_TAG, "sumDiscountOrCharge "+sumDiscountOrCharge);
        if (!isReturnOperation()) {
            CloseCheck.set_sumProd(sumProd + CloseCheck.get_sumProd()+sumDiscountOrCharge);
        }else {
            CloseCheck.set_sumReturn(sumProd + CloseCheck.get_sumReturn()+sumDiscountOrCharge);
        }

        return FiscLine.CreateFiscLine(nameTov, _count, singleCost, isDivisible, tax1,  tax2,  goodsNumber, isDiscountOrExtraCharge,  nameDiscount,  discountOrExtraChargeSum);
    }


    /*
    * Открытие чека “заказа”. Прием авансового платежа
    * */
    public void OpenOrderCheck() {
        Log.w(DBG_TAG, "OpenOrderCheck ");

        bOpenCheck = true;

        try {
            CloseCheck = new classCloseCheck();
            OrderLine = new classOrder();
        }catch (Exception e){
            Log.e(DBG_TAG, "OpenAdvanceCheck Exception "+e.getMessage());
        };
    }
    public String CreateOrderLine(String nameTov, double count, double singleCost, int isDivisible){
        DecimalFormat twoD = new DecimalFormat("#.##");
        double _count = (isDivisible==0)?count:(int)count;
        double sumProd = (double)(Double.valueOf(twoD.format(_count * singleCost).replace(",",".")));

        CloseCheck.set_sumProd(sumProd + CloseCheck.get_sumProd());

        return OrderLine.CreateOrderLine(nameTov, _count, singleCost, isDivisible);
    }

     //создание строки для регистрации оборота приема аванса.
    public String CreateOrderTakePay(String nameAdvance, double sumAdvance, int tax1, int tax2) {

        DecimalFormat twoD = new DecimalFormat("#.##");
        double _sumAdvance = (double)(Double.valueOf(twoD.format(sumAdvance).replace(",",".")));
        String _nameAdvance = ((nameAdvance.trim().length()<=128)?nameAdvance.trim():nameAdvance.trim().substring(0,128));

        String RetStr = String.format("%09d",(int)(_sumAdvance*100));

        if (tax1>0)
            RetStr += iMariaCommands.TaxName[tax1-1];
        else
            RetStr += "0";

        if (tax2>0)
            RetStr += iMariaCommands.TaxName[tax2-1];
        else
            RetStr += "0";

        RetStr += String.format("%-128s",_nameAdvance.trim());
        return RetStr.trim();
    }

    //Креатор строки для Учет авансового платежа (уменьшение оборота реализации).
    public String CreateAdvancePaymentLine(double sumAdvance, int tax1, int tax2, String ExtendedInfo){
        DecimalFormat twoD = new DecimalFormat("#.##");
        double _sumAdvance = (double)(Double.valueOf(twoD.format(sumAdvance).replace(",",".")));

        CloseCheck.set_sumProd(CloseCheck.get_sumProd() - _sumAdvance);

        String _extendedInfo = ((ExtendedInfo.trim().length()<=128)?ExtendedInfo.trim():ExtendedInfo.trim().substring(0,128));

        String RetStr = String.format("%09d",(int)(_sumAdvance*100));

        if (tax1>0)
            RetStr += iMariaCommands.TaxName[tax1-1];
        else
            RetStr += "0";

        if (tax2>0)
            RetStr += iMariaCommands.TaxName[tax2-1];
        else
            RetStr += "0";

        RetStr += String.format("%-128s",_extendedInfo.trim());
        return RetStr.trim();
    }

    //Креатор строки для Регистрация оборота возврата полной суммы ранее принятого аванса
    public String CreateReturnAdvancePaymentLine(double sumAdvance, int tax1, int tax2, String ExtendedInfo){
        DecimalFormat twoD = new DecimalFormat("#.##");
        double _sumAdvance = (double)(Double.valueOf(twoD.format(Math.abs(sumAdvance)).replace(",",".")));

        CloseCheck.set_sumReturn(CloseCheck.get_sumReturn() + ((sumAdvance>=0)?_sumAdvance:(_sumAdvance*(-1))));

        String _extendedInfo = ((ExtendedInfo.trim().length()<=128)?ExtendedInfo.trim():ExtendedInfo.trim().substring(0,128));

        String RetStr = String.format("%09d",(int)(_sumAdvance*100));

        if (tax1>0)
            RetStr += iMariaCommands.TaxName[tax1-1];
        else
            RetStr += "0";

        if (tax2>0)
            RetStr += iMariaCommands.TaxName[tax2-1];
        else
            RetStr += "0";

        RetStr += String.format("%-128s",_extendedInfo.trim());
        return RetStr.trim();
    }



    public String AccountingOfPayments(double summOp, int TypeOp, String DescribeOp, int Tax1, int Tax2, int PutGet ){

        String RetStr = "";
        try {
            DecimalFormat twoD = new DecimalFormat("#.##");
            double summaOperation = (double) (Double.valueOf(twoD.format(summOp).replace(",", ".")));


            if ((TypeOp >= 1 && TypeOp <= 3 )||(TypeOp == 4 && PutGet==1)) {
                //1 Учет платежей и передплата на бонусний рахунок
                //2 Учет платежей и подарунковий сертифікат
                //3 Учет платежей и передплата на рахунок клубної картки
                //4 Скидка на общий оборот в разрезе схем налогообложения
                CloseCheck.set_sumProd(CloseCheck.get_sumProd() + ((PutGet==0)?summaOperation:(summaOperation*(-1))) );
                ExtendedPayment extendedPayment = new ExtendedPayment();
                RetStr = extendedPayment.CreatePaymentStr(summaOperation, Tax1, Tax2, DescribeOp);
            }
        }catch (Exception e){
            Log.e(DBG_TAG, "AccountingOfPayments Exception" + e);
        }
     return RetStr;
    }

    public String getStringCOMP(){
        return CloseCheck.getStringCOMP();
    }

    public int AddPayment ( double sum , int paymentType ){

        switch (paymentType){
            case 0://наличные
                CloseCheck.set_cash(sum);
                break;
            case 1://безналичные 1
                CloseCheck.set_nonCash1(sum);
                break;
            case 2://безналичные 2
                CloseCheck.set_nonCash2(sum);
                break;
            case 3://безналичные 3
                CloseCheck.set_nonCash3(sum);
                break;
            default:
                return 1;
        }
        return 0;
    }

    public int AddPayment (  double cash, double noncash3, double noncash2, double noncash1, String idTransaction){
        CloseCheck.set_cash(cash);
        CloseCheck.set_nonCash1(noncash1);
        CloseCheck.set_nonCash2(noncash2);
        CloseCheck.set_nonCash3(noncash3);
        CloseCheck.set_NameNonCash(idTransaction);
        return 0;
    }


}


    class classCloseCheck{
        private double _sumProd;        //10
        private double _sumReturn;      //10
        private double _nonCash3 ;       //10
        private double _nonCash2 ;       //10
        private double _nonCash1 ;       //10
        private double _cash     ;       //10
        private String _NameNonCash ;   //20

        public classCloseCheck(){
           _sumProd = 0;
           _sumReturn = 0;
           _nonCash3 = 0;
           _nonCash2 = 0;
           _nonCash1 = 0;
           _cash     = 0;
           _NameNonCash= "" ;
        }

        public void set_sumProd(double sumProd) {
            this._sumProd = sumProd;
        }
        public double get_sumProd() {
            return _sumProd;
        }

        public double get_cash() {
            return _cash;
        }

        public void set_cash(double cash) {
            this._cash = cash;
        }
        public void set_NameNonCash(String NameNonCash) {
            this._NameNonCash = NameNonCash;
        }

        public double get_sumReturn() {
            return _sumReturn;
        }

        public void set_sumReturn(double sumReturn) {
            this._sumReturn = sumReturn;
        }

        public double get_nonCash1() {
            return _nonCash1;
        }

        public double get_nonCash2() {
            return _nonCash2;
        }
        public double get_nonCash3() {
            return _nonCash3;
        }

        public void set_nonCash1(double nonCash1) {
            this._nonCash1 = nonCash1;
        }

        public void set_nonCash2(double nonCash2) {
            this._nonCash2 = nonCash2;
        }

        public void set_nonCash3(double nonCash3) {
            this._nonCash3 =nonCash3;
        }

        public String getStringCOMP() {
            String RetStr ="";
         try {
             DecimalFormat twoD = new DecimalFormat("#.##");
             _sumProd = (double) (Double.valueOf(twoD.format(_sumProd).replace(",",".")));
              RetStr = String.format("%010d", (int)(_sumProd * 100));

             _sumReturn = (double) (Double.valueOf(twoD.format(_sumReturn).replace(",",".")));
             RetStr += String.format("%010d", (int)(_sumReturn * 100));

             _nonCash3 = (double) (Double.valueOf(twoD.format(_nonCash3).replace(",",".")));
             RetStr += String.format("%010d",(int)(_nonCash3 * 100));

             _nonCash2 = (double) (Double.valueOf(twoD.format(_nonCash2).replace(",",".")));
             RetStr += String.format("%010d", (int)(_nonCash2 * 100));

             _nonCash1 = (double) (Double.valueOf(twoD.format(_nonCash1).replace(",",".")));
             RetStr += String.format("%010d", (int)(_nonCash1 * 100));

             _cash = (double) (Double.valueOf(twoD.format(_cash).replace(",",".")));
             RetStr += String.format("%010d", (int)(_cash * 100));

             RetStr += String.format("%-20s", (_NameNonCash.trim().length()<=20?_NameNonCash:_NameNonCash.substring(0,20)));
         }catch(Exception e){
             Log.e(DBG_TAG, "getStringCOMP Exception: " + e.getMessage());
         }
            return RetStr;
        }
    }

    class ExtendedPayment {
        public ExtendedPayment(){}

        public String CreatePaymentStr (double sumPay, int Tax1, int Tax2, String  ExtendInfo){
            DecimalFormat twoD = new DecimalFormat("#.##");
            double sumProd = (double)(Double.valueOf(twoD.format(sumPay).replace(",",".")));
            String RetStr = String.format("%09d",(int)(sumProd*100));

            if (Tax1>0)
                RetStr += iMariaCommands.TaxName[Tax1-1];
            else
                RetStr += "0";

            if (Tax2>0)
                RetStr += iMariaCommands.TaxName[Tax2-1];
            else
                RetStr += "0";

            RetStr += String.format("%-128s",ExtendInfo);

            return RetStr;
        }
    }

    class classFiscLine{
    private int Tax1 = 0;
    private int Tax2 = 0;
    private String _nameTov = "";                 //24
    private double sumProd = 0;                   //9
    private double _singleCost = 0;               //9
    private double _count = 0;                    //6
    private int _isDivisible = 0;                 //1
    private int _roundingSchema = iMariaCommands.RoundSchema[iMariaCommands.DEF_ROUNDED_SCHEMA];              //1
    private char[] _taxSchema;                     //6 * 8
    private int _goodsNumber = 0;                 //9
    private char _isDiscountOrExtraCharge = 0;    //1
    private String _nameDiscount = "";            //13
    private double _discountOrExtraChargeSum = 0; //9
    private String _nameTovExt="";                //104


    public classFiscLine(){

        _nameTov = "";
        sumProd = 0;
        Tax1 = 0;
        Tax2 = 0;
        _singleCost = 0;
        _count = 0;
        _isDivisible = 0;
        _roundingSchema = iMariaCommands.RoundSchema[iMariaCommands.DEF_ROUNDED_SCHEMA];
        _goodsNumber = 0;

        _isDiscountOrExtraCharge = 0;
        _nameDiscount = "";
        _discountOrExtraChargeSum = 0;
        _nameTovExt="";
    }

    public String CreateFiscLine(String nameTov, double count, double singleCost, int isDivisible, int tax1, int tax2, int goodsNumber,int isDiscountOrExtraCharge, String nameDiscount, double discountOrExtraChargeSum )
    {
        DecimalFormat twoD = new DecimalFormat("#.##");
        _nameTov = ((nameTov.trim().length()<=24)?nameTov.trim():nameTov.trim().substring(0,24));
        _count = count;
        _singleCost = singleCost;
        Tax1 = tax1;
        Tax2 = tax2;
        _goodsNumber = goodsNumber;
        _isDivisible = isDivisible;
        _nameTovExt = ((nameTov.trim().length()<24)?"":nameTov.trim().substring(24,nameTov.trim().length()));
        _nameTovExt = (_nameTovExt.length()>104)?_nameTovExt.substring(0,104):_nameTovExt.trim();

        _isDiscountOrExtraCharge =  (isDiscountOrExtraCharge==0)?'0':(isDiscountOrExtraCharge==1)?'-':'+';
        _nameDiscount = ((nameDiscount.trim().length() <= 13) ? nameDiscount : nameDiscount.trim().substring(0, 13));
        _discountOrExtraChargeSum = (double) (Double.valueOf(twoD.format(discountOrExtraChargeSum).replace(",", ".")));

        String RetStr = String.format("%-24s", _nameTov.trim());


        sumProd = (double)(Double.valueOf(twoD.format(_count * _singleCost).replace(",",".")));
        RetStr += String.format("%09d",(int)(sumProd*100));

        _singleCost  = (double)(Double.valueOf(twoD.format(sumProd /_count).replace(",",".")));
        RetStr += String.format("%09d",(int)(_singleCost*100));

        RetStr += String.format("%06d",(int)((_isDivisible==0)?_count*1000:_count));
        RetStr += String.format("%1d",_isDivisible);
        RetStr += String.format("%1d",_roundingSchema);

        if (Tax1>0)
            RetStr += iMariaCommands.TaxName[Tax1-1];
        else
            RetStr += "0";

        if (Tax2>0)
           RetStr += iMariaCommands.TaxName[Tax2-1];
        else
           RetStr += "0";

        RetStr += String.format("%09d",_goodsNumber);


        RetStr += String.format("%c",_isDiscountOrExtraCharge);
        if ((isDiscountOrExtraCharge!=0)) {
            RetStr += String.format("%-13s", _nameDiscount);
            RetStr += String.format("%09d", (int) (_discountOrExtraChargeSum * 100));
        }else {
            RetStr += String.format("%013d", (int)0);
            RetStr += String.format("%09d", (int)(0));
        }
        RetStr += String.format("%-104s",_nameTovExt);
        return RetStr.trim();
    }

}

    class classOrder{
    private String _nameTov = "";                 //24
    //private double sumProd = 0;                   //9
    private double _singleCost = 0;               //9
    private double _count = 0;                    //6
    private int _isDivisible = 0;                 //1
    private int _roundingSchema = iMariaCommands.RoundSchema[iMariaCommands.DEF_ROUNDED_SCHEMA];              //1


    public classOrder(){

        _nameTov = "";
        _singleCost = 0;
        _count = 0;
        _isDivisible = 0;
        _roundingSchema = iMariaCommands.RoundSchema[iMariaCommands.DEF_ROUNDED_SCHEMA];

    }

    public String CreateOrderLine(String nameTov, double count, double singleCost, int isDivisible )
    {
        DecimalFormat twoD = new DecimalFormat("#.##");
        _nameTov = ((nameTov.trim().length()<=32)?nameTov.trim():nameTov.trim().substring(0,32));
        _count = count;
        _singleCost = singleCost;
        _isDivisible = isDivisible;

        _singleCost  = (double)(Double.valueOf(twoD.format(_singleCost).replace(",",".")));
        String RetStr = String.format("%09d",(int)(_singleCost*100));

        RetStr += String.format("%06d",(int)((_isDivisible==0)?_count*1000:_count));
        RetStr += String.format("%1d",_isDivisible);
        RetStr += String.format("%1d",_roundingSchema);

        RetStr += String.format("%-24s", _nameTov.trim());
       return RetStr.trim();
    }

}



    class Tax{

        private char TaxChar;
        private int TypeTax;//   0/1/2   вложенный/наложенный с добавлением/наложенный с вычитанием (“подоходный”).
        private String TaxRate; //ставка налога

        public Tax(char taxname, int typetax, String taxRate){
            this.TaxChar = taxname;
            this.TypeTax = typetax;
            this.TaxRate = taxRate;
        }

        String getTax(){
            return String.format("%1c%1d%4s",TaxChar,TypeTax,TaxRate);
        }
    }

