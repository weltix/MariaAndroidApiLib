package com.resonance.ekka.mariaapilib;

import java.nio.charset.Charset;

public interface iMariaCommands {
    public final char[] TaxName = new char[]{'А','Б','В','Г','Д','Е','Ж','З'};
    //public static final Charset ENCODING_CHARSET = Charset.forName("windows-1251");//для отладки
    public static final Charset ENCODING_CHARSET = Charset.forName("cp866");//для работы с кассой
    public final int DEF_ROUNDED_SCHEMA = 0;
    public int[] RoundSchema = new int[]{0,1,2};

    public final String DBG_TAG = "DBG_TAG";

    final char UKR_UPPER_I = 'І';
    final char UKR_LOWER_i = 'і';
    final char ENG_UPPER_I = 'I';
    final char ENG_LOWER_i = 'i';
    final char UKR_UPPER_G = 'Ґ';
    final char UKR_LOWER_g = 'ґ';
    final char RUS_UPPER_G = 'I';
    final char RUS_LOWER_g = 'i';


    final String CMD_UPAS = "UPAS";
    final String CMD_PRTX = "PRTX";
    final String CMD_SLPB = "SLPB";
    final String CMD_FINF = "FINF";
    final String CMD_FEED = "FEED";
    final String CMD_COPY = "COPY";
    final String CMD_TEXT = "TEXT";
    final String CMD_NLPR = "NLPR";
    final String CMD_CAIO = "CAIO";
    final String CMD_NULL = "NULL";
    final String CMD_nrep = "nrep";
    final String CMD_PPOD = "PPOD";
    final String CMD_PCOD = "PCOD";
    final String CMD_ARTZ = "ARTZ";
    final String CMD_DIZV = "DIZV";
    final String CMD_IREN = "IREN";
    final String CMD_FIRN = "FIRN";
    final String CMD_ZREP = "ZREP";
    final String CMD_NREP = "NREP";
    final String CMD_IREP = "IREP";
    final String CMD_FIRP = "FIRP";
    final String CMD_CUTR = "CUTR";
    final String CMD_ARMO = "ARMO";
    final String CMD_BOTM = "BOTM";
    final String CMD_BOTm = "BOTm";
    final String CMD_HEAD = "HEAD";
    final String CMD_DEPT = "DEPT";
    final String CMD_NPDI = "NPDI";
    final String CMD_ZDNM = "ZDNM";
    final String CMD_KASS = "KASS";
    final String CMD_GRBG = "GRBG";
    final String CMD_GREN = "GREN";
    final String CMD_CTIM = "CTIM";
    final String CMD_CTMP = "CTMP";
    final String CMD_CTMM = "CTMM";
    final String CMD_ARTD = "ARTD";
    final String CMD_CCAS = "CCAS";
    final String CMD_CFIS = "CFIS";
    final String CMD_GLCN = "GLCN";
    final String CMD_GETD = "GETD";
    final String CMD_CONf = "CONf";
    final String CMD_MDMD = "MDMD";
    final String CMD_DIsp = "DIsp";
    final String CMD_PREP = "PREP";
    final String CMD_BCHN = "BCHN";
    final String CMD_TMOD = "TMOD";
    final String CMD_GCPR = "GCPR";
    final String CMD_GCLN = "GCLN";
    final String CMD_CTXT = "CTXT";
    final String CMD_DBEG = "DBEG";
    final String CMD_CANC = "CANC";
    final String CMD_COMP = "COMP";
    final String CMD_FICD = "FICD";
    final String CMD_BFCD = "BFCD";
    final String CMD_FISC = "FISC";
    final String CMD_CNAL = "CNAL";
    final String CMD_CNAM = "CNAM";
    final String CMD_BONS = "BONS";
    final String CMD_SERT = "SERT";
    final String CMD_CLCR = "CLCR";
    final String CMD_mBNS = "mBNS";//Учет платежей с бонусного счета (уменьшение оборота  реализации-возврата)
    final String CMD_mSRT = "mSRT";//Учет платежа за подарочный сертификат (уменьшение оборота реализации-возврата)
    final String CMD_mCLC = "mCLC";//Учет платежей с клубной карты (уменьшение оборота реализации-возврата)
    final String CMD_mDIS = "mDIS";//Скида на общий оборот в разрезе схем налогообложения. (уменьшение оборота реализации-возврата)
    final String CMD_PRAV = "PRAV";//Открытие чека “заказа”. Прием авансового платежа.
    final String CMD_AVNS = "AVNS";//Регистрация оборота приема аванса.
    final String CMD_AVLS = "AVLS";//Печать описания одной заказываемой позиции товара(услуги) в пределах чека заказа.
    final String CMD_CLAV = "CLAV";//Открытие чека “выполнения заказа”. Расчет по авансовому платежу.
    final String CMD_mAVN = "mAVN";//Учет авансового платежа (уменьшение оборота реализации).
    final String CMD_SPKF = "SPKF";//Принудительно передать данные на эквайер
    final String CMD_GCQR = "GCQR";//Формирование символа QR-кода.
    final String CMD_PRBA = "PRBA";//Открытие чека “возврата аванса”. Расчет по авансовомуплатежу.
    final String CMD_BAAV = "BAAV";//Регистрация оборота возврата полной суммы ранее принятого аванса
    final String CMD_BAAR = "BAAR";//Учет сумм отпущенных товаров-услуг при возврате аванса
    final String CMD_TKTP = "TKTP";//Открытие чека продажи (возврата) билета (багажной китанции).
    final String CMD_TKSM = "TKSM";//Ввод сумм составляющих стоимости билета (квитанции)
    final String CMD_TKMS = "TKMS";//Ввод суммы  удержания
    final String CMD_TKCL = "TKCL";//Закрытие чека билета  (багажной квитанции)
    final String CMD_TKRC = "TKRC";//Ввод описания маршрута.
    final String CMD_NALG = "NALG";//Схемы налогообложения
    final String CMD_NNAM = "NNAM";//Установка наименования налога
    final String CMD_VBAT = "MTMT:vbat?";//показать состояние литиевого аккумулятора (только для РРО KCT-M)
    final String CMD_PSDT = "PSDT";//Ввод данных по слипу после успешной трансакции по платежному терминалу.
    final String ANSW_READY = "READY";

}
