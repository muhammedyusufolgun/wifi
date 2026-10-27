package tr.olgun.wifiharita
import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.wifi.*
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*

class MainActivity:Activity(){
 private val green=Color.rgb(0,112,47)
 private val gray=Color.rgb(105,117,130)
 private lateinit var wifi:WifiManager
 private lateinit var root:LinearLayout
 private lateinit var current:LinearLayout
 private lateinit var results:LinearLayout
 private var detail=false
 private var pullStart=0f
 private var pullReady=false
 private val handler=Handler(Looper.getMainLooper())
 private val scanReceiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){if(!detail)showNetworks()}}
 private fun dp(x:Int)=(x*resources.displayMetrics.density).toInt()
 private fun t(s:String,size:Float=15f,bold:Boolean=false,color:Int=Color.rgb(25,39,54))=TextView(this).apply{
  text=s;textSize=size;setTextColor(color);setPadding(dp(4),dp(6),dp(4),dp(6))
  if(bold)setTypeface(null,android.graphics.Typeface.BOLD)
 }
 private fun col()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
 private fun add(p:LinearLayout,v:View,margin:Int=0){p.addView(v,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(margin)})}
 private fun panel()=col().apply{
  setPadding(dp(14),dp(12),dp(14),dp(12))
  background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(13).toFloat();setStroke(dp(1),Color.rgb(223,231,238))}
 }
 private fun quality(n:Int)=when{n>=-49->"Mükemmel";n>=-59->"Çok iyi";n>=-67->"İyi";n>=-74->"Orta";n>=-79->"Zayıf";n>=-89->"Çok zayıf";else->"Kritik"}
 private fun color(n:Int)=when{n>=-59->green;n>=-67->Color.rgb(101,163,13);n>=-74->Color.rgb(224,158,0);n>=-79->Color.rgb(234,100,50);else->Color.RED}
 private fun band(n:Int)=when{n>=5925->"6 GHz";n>=4900->"5 GHz";else->"2.4 GHz"}
 private fun channel(n:Int)=when{n==2484->14;n in 2412..2472->(n-2407)/5;n>=5955->(n-5950)/5;n>=5000->(n-5000)/5;else->0}
 private fun bars(n:Int)=object:View(this){
  val paint=Paint(3)
  override fun onMeasure(w:Int,h:Int){setMeasuredDimension(dp(82),dp(55))}
  override fun onDraw(c:Canvas){
   val level=((n+95)*5/65).coerceIn(1,5)
   for(i in 0..4){val h=dp(10+i*10);paint.color=if(i<level)color(n) else Color.rgb(218,223,230)
    c.drawRoundRect(dp(i*16+2).toFloat(),(height-h).toFloat(),dp(i*16+12).toFloat(),height.toFloat(),dp(3).toFloat(),dp(3).toFloat(),paint)}
  }
 }
 private fun layout(title:String){
  val frame=col().apply{setBackgroundColor(Color.rgb(248,250,252))}
  add(frame,t(title,24f,true).apply{setPadding(dp(18),dp(16),0,dp(14));setBackgroundColor(Color.WHITE)})
  val scroll=ScrollView(this).apply{
   isFillViewport=true
   setOnTouchListener { v,event ->
    when(event.actionMasked){
     MotionEvent.ACTION_DOWN -> {pullStart=event.y;pullReady=false}
     MotionEvent.ACTION_MOVE -> {if(!detail && scrollY==0 && event.y-pullStart>dp(85))pullReady=true}
     MotionEvent.ACTION_UP -> {if(pullReady && !detail){pullReady=false;scan();Toast.makeText(this@MainActivity,"Wi-Fi ağları yenileniyor",Toast.LENGTH_SHORT).show()}}
    }
    false
   }
  }
  root=col().apply{setPadding(dp(16),dp(12),dp(16),dp(12))}
  scroll.addView(root);frame.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  add(frame,t("Muhammed Yusuf OLGUN",11f,false,Color.rgb(92,99,108)).apply{gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(10))})
  setContentView(frame)
 }
 override fun onCreate(state:Bundle?){
  super.onCreate(state)
  window.statusBarColor=Color.WHITE;window.navigationBarColor=Color.WHITE
  window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  wifi=applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
  registerReceiver(scanReceiver,IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
  home()
  if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
   requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),12)
  else scan()
  handler.post(object:Runnable{override fun run(){if(!detail)updateCurrent();handler.postDelayed(this,3000)}})
 }
 private fun home(){
  detail=false;layout("Wi-Fi Sinyal Haritası")
  current=panel();add(root,current)
  val heading=row()
  heading.addView(t("Diğer Wi-Fi Ağları",19f,true),LinearLayout.LayoutParams(0,-2,1f))
  add(root,heading,16)
  add(root,t("Listeyi aşağı çekerek yenileyin.",12f,false,gray))
  results=panel();add(root,results)
  updateCurrent();showNetworks()
 }
 private fun updateCurrent(){
  current.removeAllViews();add(current,t("Bağlı Olunan Ağ",16f,true))
  if(!wifi.isWifiEnabled){add(current,t("Wi-Fi kapalı"));return}
  @Suppress("DEPRECATION") val info=wifi.connectionInfo
  val n=info.rssi
  if(n<=-127){add(current,t("Bağlı ağ bulunamadı"));return}
  val name=info.ssid.trim('"').let{if(it=="<unknown ssid>"||it.isBlank())"Bağlı Wi-Fi" else it}
  val head=row()
  head.addView(t("◉  "+name,19f,true,green),LinearLayout.LayoutParams(0,-2,1f))
  head.addView(t("Bağlı",13f,true,green))
  add(current,head,10)
  val r=row();val left=col()
  add(left,t(n.toString()+" dBm",35f,true,color(n)))
  add(left,t(quality(n),20f,true,color(n)))
  r.addView(left,LinearLayout.LayoutParams(0,-2,1f));r.addView(bars(n))
  add(current,r,12)
  add(current,t(band(info.frequency)+" · Kanal "+channel(info.frequency)+" · "+info.linkSpeed+" Mbps",13f,false,gray),8)
  add(current,t("Sinyal seviyesi: "+quality(n),13f,false,green),7)
 }
 private fun locationEnabled():Boolean{
  val lm=getSystemService(Context.LOCATION_SERVICE) as LocationManager
  return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)||lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
 }
 private fun scan(){
  if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),12);return
  }
  if(!locationEnabled()){Toast.makeText(this,"Konum hizmetini açın.",Toast.LENGTH_LONG).show()
   startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));return}
  @Suppress("DEPRECATION") wifi.startScan()
  showNetworks()
 }
 private fun showNetworks(){
  results.removeAllViews()
  if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
   add(results,t("Diğer ağlar için uygulamayı kullanırken konum izni verin.",13f,false,gray));return}
  if(!locationEnabled()){add(results,t("Diğer ağları taramak için Konum hizmetini açın.",13f,false,gray));return}
  @Suppress("DEPRECATION") val items=try{wifi.scanResults.sortedByDescending{it.level}}catch(e:SecurityException){emptyList()}
  if(items.isEmpty()){add(results,t("Ağ bulunamadı. Listeyi aşağı çekerek yenileyin.",13f,false,gray));return}
  for(ap in items){
   val r=row().apply{setPadding(0,dp(8),0,dp(8))}
   r.addView(t("◉",22f,true,green))
   val left=col()
   add(left,t(ap.SSID.ifBlank{"Gizli ağ"},14f,true))
   add(left,t(band(ap.frequency)+" · Kanal "+channel(ap.frequency),11f,false,gray))
   r.addView(left,LinearLayout.LayoutParams(0,-2,1f))
   val right=col()
   add(right,t(ap.level.toString()+" dBm",14f,true))
   add(right,t(quality(ap.level),12f,true,color(ap.level)))
   r.addView(right)
   r.addView(bars(ap.level),LinearLayout.LayoutParams(dp(82),dp(55)))
   r.addView(t("›",23f,false,gray))
   r.setOnClickListener{showDetail(ap)}
   add(results,r)
   add(results,View(this).apply{setBackgroundColor(Color.rgb(235,239,244));layoutParams=LinearLayout.LayoutParams(-1,dp(1))})
  }
 }
 private fun line(p:LinearLayout,key:String,value:String){
  val r=row().apply{setPadding(0,dp(7),0,dp(7))}
  r.addView(t(key,14f,false,gray),LinearLayout.LayoutParams(0,-2,1f))
  r.addView(t(value,14f,true));add(p,r)
 }
 private fun openWifiSettings(){
  // Android does not permit ordinary apps to silently join arbitrary Wi-Fi networks.
  // System Wi-Fi settings lets the user choose and authorize the connection.
  startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
 }
 private fun showDetail(ap:ScanResult){
  detail=true;layout("Ağ Detayı")
  add(root,t("← Geri",15f,true,green).apply{setOnClickListener{home()} })
  val c=panel()
  add(c,t("◉  "+ap.SSID.ifBlank{"Gizli ağ"},23f,true,green))
  add(c,t(ap.level.toString()+" dBm",35f,true,color(ap.level)),12)
  add(c,t(quality(ap.level),19f,true,color(ap.level)))
  add(c,bars(ap.level))
  add(root,c,12)
  val info=panel()
  add(info,t("Ağ Bilgileri",18f,true))
  line(info,"SSID",ap.SSID.ifBlank{"Gizli ağ"})
  line(info,"BSSID",ap.BSSID)
  line(info,"Frekans",ap.frequency.toString()+" MHz ("+band(ap.frequency)+")")
  line(info,"Kanal",channel(ap.frequency).toString())
  line(info,"Sinyal Gücü",ap.level.toString()+" dBm")
  line(info,"Sinyal Seviyesi",quality(ap.level))
  line(info,"Güvenlik",when{ap.capabilities.contains("SAE")->"WPA3";ap.capabilities.contains("WPA2")->"WPA2";ap.capabilities.contains("WPA")->"WPA";ap.capabilities.contains("WEP")->"WEP";else->"Açık / bilinmiyor"})
  add(root,info,14)
  add(root,Button(this).apply{text="Bu Ağa Bağlan";setOnClickListener{openWifiSettings()}},14)
 }
 override fun onBackPressed(){if(detail)home()else super.onBackPressed()}
 override fun onRequestPermissionsResult(code:Int,p:Array<out String>,g:IntArray){
  super.onRequestPermissionsResult(code,p,g)
  if(code==12&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)scan()
  else if(!detail)showNetworks()
 }
 override fun onDestroy(){handler.removeCallbacksAndMessages(null);unregisterReceiver(scanReceiver);super.onDestroy()}
}