package tr.olgun.wifiharita

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.widget.*
import android.view.Gravity

class MainActivity:Activity(){
 private lateinit var wifi:WifiManager
 private lateinit var connected:LinearLayout
 private lateinit var others:LinearLayout
 private val handler=Handler(Looper.getMainLooper())
 private val green=Color.rgb(0,110,47)
 private fun text(s:String,sp:Float=15f,bold:Boolean=false):TextView=TextView(this).apply{
  text=s;textSize=sp;setTextColor(Color.rgb(28,42,55))
  if(bold)setTypeface(null,Typeface.BOLD)
  setPadding(8,7,8,7)
 }
 private fun quality(v:Int)=when {
  v>=-49->"Mükemmel";v>=-59->"Çok iyi";v>=-67->"İyi";v>=-74->"Orta"
  v>=-79->"Zayıf";v>=-89->"Çok zayıf";else->"Kritik"
 }
 private fun color(v:Int)=when{
  v>=-59->green;v>=-67->Color.rgb(101,163,13);v>=-74->Color.rgb(202,138,4)
  v>=-79->Color.rgb(234,88,12);else->Color.rgb(220,38,38)
 }
 private fun card():LinearLayout=LinearLayout(this).apply{
  orientation=LinearLayout.VERTICAL;setPadding(14,12,14,12)
  background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=20f;setStroke(1,Color.rgb(222,230,238))}
 }
 private fun band(freq:Int)=when{freq>=5925->"6 GHz";freq>=4900->"5 GHz";freq>=2400->"2.4 GHz";else->"$freq MHz"}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  wifi=applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
  val scroll=ScrollView(this).apply{setBackgroundColor(Color.rgb(248,250,252))}
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,22,16,24)}
  scroll.addView(root);setContentView(scroll)
  root.addView(text("WiFi Sinyal Haritası",24f,true))
  root.addView(text("Yerel Wi-Fi sinyal ölçümü · Veri gönderilmez",12f))
  root.addView(text("BAĞLI OLUNAN AĞ",13f,true))
  connected=card();root.addView(connected)
  root.addView(text("Diğer Wi-Fi Ağları",21f,true))
  val refresh=Button(this).apply{text="Yenile";setOnClickListener{update()}}
  root.addView(refresh)
  others=card();root.addView(others)
  root.addView(text("Gizlilik: Konum izni istenmez. Android, yakındaki Wi-Fi ağ adlarını taramak için konum izni gerektirdiğinden diğer ağlar bu izin olmadan listelenemez.",12f))
  update()
  handler.post(object:Runnable{override fun run(){updateConnected();handler.postDelayed(this,3000)}})
 }
 private fun updateConnected(){
  connected.removeAllViews()
  if(!wifi.isWifiEnabled){connected.addView(text("Wi-Fi kapalı",19f,true));return}
  @Suppress("DEPRECATION") val info=wifi.connectionInfo
  val rssi=info.rssi
  if(rssi<=-127){connected.addView(text("Wi-Fi bağlantısı bulunamadı",18f,true));return}
  val name=info.ssid.trim('"').let{if(it=="<unknown ssid>"||it.isBlank())"Bağlı Wi-Fi ağı" else it}
  connected.addView(text("◉  "+name,20f,true).apply{setTextColor(green)})
  connected.addView(text("$rssi dBm",35f,true).apply{setTextColor(color(rssi))})
  connected.addView(text(quality(rssi),19f,true).apply{setTextColor(color(rssi))})
  val strength=((rssi+95)*5/65).coerceIn(1,5)
  connected.addView(text((1..5).joinToString("  "){if(it<=strength)"▮" else "▯"},27f,true).apply{setTextColor(color(rssi))})
  connected.addView(text(band(info.frequency)+"  ·  "+info.frequency+" MHz  ·  "+info.linkSpeed+" Mbps",13f))
  connected.addView(text("Bağlantı durumu: Bağlı",13f,true))
 }
 private fun update(){
  updateConnected()
  others.removeAllViews()
  others.addView(text("Yakındaki ağlar listelenemiyor",17f,true))
  others.addView(text("Konum izni istememe tercihiniz korunuyor. Android, diğer ağların SSID ve dBm tarama sonuçlarına bu izin olmadan erişimi kısıtlar.",13f))
 }
}