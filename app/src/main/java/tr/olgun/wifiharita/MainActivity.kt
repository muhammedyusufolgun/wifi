package tr.olgun.wifiharita
import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.widget.*
import android.graphics.drawable.GradientDrawable

class MainActivity:Activity(){
 private lateinit var wifi:WifiManager
 private lateinit var main:LinearLayout
 private lateinit var signal:TextView
 private lateinit var networks:LinearLayout
 private val handler=Handler(Looper.getMainLooper())
 private fun text(value:String,size:Float=16f):TextView=TextView(this).apply{
  text=value;textSize=size;setTextColor(Color.rgb(30,45,60));setPadding(12,10,12,10)
 }
 private fun quality(v:Int):String=when{
  v>=-49->"Mükemmel";v>=-59->"Çok iyi";v>=-67->"İyi"
  v>=-74->"Orta";v>=-79->"Zayıf";v>=-89->"Çok zayıf";else->"Kritik"
 }
 private fun color(v:Int):Int=when{
  v>=-59->Color.rgb(22,163,74);v>=-67->Color.rgb(101,163,13)
  v>=-74->Color.rgb(202,138,4);v>=-79->Color.rgb(234,88,12)
  else->Color.rgb(220,38,38)
 }
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  wifi=applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
  val scroll=ScrollView(this);scroll.setBackgroundColor(Color.rgb(248,250,252))
  main=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,20,18,20)}
  scroll.addView(main);setContentView(scroll)
  main.addView(text("WiFi Sinyal Haritası",25f).apply{setTypeface(null,Typeface.BOLD)})
  main.addView(text("Profesyonel Wi-Fi Analizörü",13f))
  signal=text("Ölçülüyor...",29f);signal.setTypeface(null,Typeface.BOLD);main.addView(signal)
  main.addView(text("Diğer Wi-Fi Ağları",22f))
  main.addView(Button(this).apply{text="Ağları Yenile";setOnClickListener{scan()}})
  networks=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};main.addView(networks)
  if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
   requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),17)
  else scan()
  handler.post(object:Runnable{override fun run(){refreshSignal();handler.postDelayed(this,3000)}})
 }
 private fun refreshSignal(){
  if(!wifi.isWifiEnabled){signal.text="Wi-Fi kapalı";return}
  @Suppress("DEPRECATION") val info=wifi.connectionInfo
  val rssi=info.rssi
  if(rssi<=-127){signal.text="Wi-Fi bağlı değil";return}
  signal.text=info.ssid.trim('"')+"\n"+rssi+" dBm · "+quality(rssi)+"\n"+info.frequency+" MHz"
  signal.setTextColor(color(rssi))
 }
 private fun scan(){
  if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),17);return
  }
  if(!wifi.isWifiEnabled){networks.removeAllViews();networks.addView(text("Wi-Fi kapalı"));return}
  @Suppress("DEPRECATION") wifi.startScan()
  networks.removeAllViews()
  @Suppress("DEPRECATION") val results=wifi.scanResults.sortedByDescending{it.level}
  if(results.isEmpty())networks.addView(text("Tarama sonucu yok. Konum hizmetlerini açıp tekrar deneyin."))
  for(ap in results){
   val line=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
   val name=ap.SSID.ifBlank{"Gizli ağ"}
   line.addView(text(name+"\n"+ap.frequency+" MHz",14f),LinearLayout.LayoutParams(0,-2,1f))
   line.addView(text(ap.level.toString()+" dBm\n"+quality(ap.level),13f).apply{setTextColor(color(ap.level))})
   line.background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=12f;setStroke(1,Color.LTGRAY)}
   val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,4,0,4)
   networks.addView(line,lp)
  }
 }
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==17&&grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)scan()
 }
}