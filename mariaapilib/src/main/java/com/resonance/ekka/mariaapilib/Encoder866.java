package com.resonance.ekka.mariaapilib;

import android.util.Log;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


import static com.resonance.ekka.mariaapilib.iMariaCommands.ENG_LOWER_i;
import static com.resonance.ekka.mariaapilib.iMariaCommands.ENG_UPPER_I;
import static com.resonance.ekka.mariaapilib.iMariaCommands.RUS_LOWER_g;
import static com.resonance.ekka.mariaapilib.iMariaCommands.RUS_UPPER_G;
import static com.resonance.ekka.mariaapilib.iMariaCommands.UKR_LOWER_g;
import static com.resonance.ekka.mariaapilib.iMariaCommands.UKR_LOWER_i;
import static com.resonance.ekka.mariaapilib.iMariaCommands.UKR_UPPER_G;
import static com.resonance.ekka.mariaapilib.iMariaCommands.UKR_UPPER_I;

public class Encoder866 {



    private static Map<Character, Byte> ENCODING_CP866 = new HashMap<Character, Byte>() {{
        put('А',(byte)-128);
        put('Б',(byte)-127);
        put('В',(byte)-126);
        put('Г',(byte)-125);
        put('Д',(byte)-124);
        put('Е',(byte)-123);
        put('Ж',(byte)-122);
        put('З',(byte)-121);
        put('И',(byte)-120);
        put('Й',(byte)-119);
        put('К',(byte)-118);
        put('Л',(byte)-117);
        put('М',(byte)-116);
        put('Н',(byte)-115);
        put('О',(byte)-114);
        put('П',(byte)-113);
        put('Р',(byte)-112);
        put('С',(byte)-111);
        put('Т',(byte)-110);
        put('У',(byte)-109);
        put('Ф',(byte)-108);
        put('Х',(byte)-107);
        put('Ц',(byte)-106);
        put('Ч',(byte)-105);
        put('Ш',(byte)-104);
        put('Щ',(byte)-103);
        put('Ъ',(byte)-102);
        put('Ы',(byte)-101);
        put('Ь',(byte)-100);
        put('Э',(byte)-99);
        put('Ю',(byte)-98);
        put('Я',(byte)-97);
        put('а',(byte)-96);
        put('б',(byte)-95);
        put('в',(byte)0xA2);
        put('г',(byte)0xA3/*-93*/);
        put('д',(byte)0xA4/*-92*/);
        put('е',(byte)0xA5/*-91*/);
        put('ж',(byte)0xA6/*-90*/);
        put('з',(byte)0xA7/*-89*/);
        put('и',(byte)0xA8/*-88*/);
        put('й',(byte)0xA9/*-87*/);
        put('к',(byte)0xAA/*-86*/);
        put('л',(byte)0xAB/*-85*/);
        put('м',(byte)0xAC/*-84*/);
        put('н',(byte)0xAD/*-83*/);
        put('о',(byte)0xAE/*-82*/);
        put('п',(byte)0xAF/*-81*/);
        put('░',(byte)-80);
        put('▒',(byte)-79);
        put('▓',(byte)-78);
        put('│',(byte)-77);
        put('┤',(byte)-76);
        put('╡',(byte)-75);
        put('╢',(byte)-74);
        put('╖',(byte)-73);
        put('╕',(byte)-72);
        put('╣',(byte)-71);
        put('║',(byte)-70);
        put('╗',(byte)-69);
        put('╝',(byte)-68);
        put('╜',(byte)-67);
        put('╛',(byte)-66);
        put('┐',(byte)-65);
        put('└',(byte)-64);
        put('┴',(byte)-63);
        put('┬',(byte)-62);
        put('├',(byte)-61);
        put('─',(byte)-60);
        put('┼',(byte)-59);
        put('╞',(byte)-58);
        put('╟',(byte)-57);
        put('╚',(byte)-56);
        put('╔',(byte)-55);
        put('╩',(byte)-54);
        put('╦',(byte)-53);
        put('╠',(byte)-52);
        put('═',(byte)-51);
        put('╬',(byte)-50);
        put('╧',(byte)-49);
        put('╨',(byte)-48);
        put('╤',(byte)-47);
        put('╥',(byte)-46);
        put('╙',(byte)-45);
        put('╘',(byte)-44);
        put('╒',(byte)-43);
        put('╓',(byte)-42);
        put('╫',(byte)-41);
        put('╪',(byte)-40);
        put('┘',(byte)-39);
        put('┌',(byte)-38);
        put('█',(byte)-37);
        put('▄',(byte)-36);
        put('▌',(byte)-35);
        put('▐',(byte)-34);
        put('▀',(byte)-33);
        put('р',(byte)-32);
        put('с',(byte)-31);
        put('т',(byte)-30);
        put('у',(byte)-29);
        put('ф',(byte)-28);
        put('х',(byte)-27);
        put('ц',(byte)-26);
        put('ч',(byte)-25);
        put('ш',(byte)-24);
        put('щ',(byte)-23);
        put('ъ',(byte)-22);
        put('ы',(byte)-21);
        put('ь',(byte)-20);
        put('э',(byte)-19);
        put('ю',(byte)-18);
        put('я',(byte)-17);
        put('Ё',(byte)-16);
        put('ё',(byte)-15);
        put('Є',(byte)0xF2/*-12*/);
        put('є',(byte)0xF3/*-11*/);
        put('Ї',(byte)0xF4/*-8*/);
        put('ї',(byte)0xF5/*-7*/);
        put('Ў',(byte)0xF6);
        put('ў',(byte)0xF7);

        put('Ґ',(byte)0x83);
        put('ґ',(byte)0xA3);

        put('·',(byte)-6);
        put('√',(byte)-5);
        put('№',(byte)-4);
        put('¤',(byte)-3);
        put('■',(byte)-2);
        put(' ',(byte)-1);
    }};
    private static Map<Byte, Character> REVERSE = new HashMap<>();

    static{
        for (Character c : ENCODING_CP866.keySet()) {

            REVERSE.put(ENCODING_CP866.get(c), c);
           // Log.d(DBG_TAG, "Enc866 :" + c+ "> "+String.format("%d",(byte)c.charValue()) );
        }
    }

   /* public static String from866Byte(byte b){
        String res="";
        if(REVERSE.containsKey(b))
           res += REVERSE.get(b);
        else {
           res += (char) b;
        }
        return res;
    }*/

    public static String from866Bytes(byte[] data){
        if (data==null)return "";
        String res="";
        for (int i = 0; i < data.length; i++) {
            if(REVERSE.containsKey(data[i])) {
                res += REVERSE.get(data[i]);
            }
            else {

                if ((char) data[i]==UKR_UPPER_I)
                    res +=  ENG_UPPER_I;

                else if ((char) data[i]==UKR_LOWER_i)
                    res +=  ENG_LOWER_i;

                else
                    res += (char) data[i];
            }
        }
        return res;
    }
    public static byte[] get866Bytes(String data){
        if(data==null)return new byte[0];
        int len=data.length();
        byte[] res = new byte[len];
        for (int i = 0; i <len; i++) {
            if(ENCODING_CP866.containsKey(data.charAt(i))) {
                res[i] = ENCODING_CP866.get(data.charAt(i));
               // Log.d(DBG_TAG, "Encoder866 :" + (byte) data.charAt(i)+"["+res[i]+"]");
            }else  {
                if ((char) data.charAt(i)==UKR_UPPER_I)
                    res[i] =  ENG_UPPER_I;

                else if ((char) data.charAt(i)==UKR_LOWER_i)
                    res[i] =  ENG_LOWER_i;

                else
                    res[i]= (byte) data.charAt(i);
            }
        }
        return res;
    }


}
