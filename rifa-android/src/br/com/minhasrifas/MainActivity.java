package br.com.minhasrifas;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    JSONArray raffles = new JSONArray();
    JSONObject current;
    LinearLayout body;
    String exportText, pendingImage;
    ImageView imagePreview;
    Handler onlineHandler=new Handler();
    boolean onlineBusy=false,foreground=false,viewingRequests=false;
    int onlineCursor=0;
    JSONObject profile;
    boolean accountBusy=false,managingOrders=false,showAcceptedOrders=false;
    long editCounter=0;
    Runnable backupJob=new Runnable(){public void run(){
        if(profile==null||!foreground)return;
        if(onlineBusy||accountBusy||managingOrders){onlineHandler.postDelayed(this,500);return;}
        accountSync(false);
    }};
    int pollTurn=0;
    Runnable onlinePoll=new Runnable(){public void run(){
        if(!foreground||profile==null)return;
        if(!onlineBusy&&!accountBusy&&!managingOrders){
            boolean hasOnline=false;for(int i=0;i<raffles.length();i++)if(raffles.optJSONObject(i).has("webId"))hasOnline=true;
            // Alternate account work with order work; never start both in the same cycle.
            if(!hasOnline||pollTurn++%3==0)syncTheme(false);
            else for(int i=0;i<raffles.length();i++){JSONObject r=raffles.optJSONObject((onlineCursor+i)%raffles.length());if(r.has("webId")){onlineCursor=(onlineCursor+i+1)%raffles.length();syncOnline(r,false,null);break;}}
        }
        onlineHandler.postDelayed(this,30000);
    }};
    boolean visualSettingsOpen=false;
    final String[] themeIds={"green","red","pink","yellow","blue","purple"};
    final int[] themeColors={0xff185b49,0xffb12626,0xffad2364,0xfff5c328,0xff2058ad,0xff6e30a1};
    int green = Color.rgb(24,91,73);
    final int ink = Color.rgb(28,43,38);
    int accentText(){return green==Color.rgb(245,195,40)?ink:Color.WHITE;}
    int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        green=getPreferences(0).getInt("accentColor",green);
        getWindow().setStatusBarColor(green);
        getWindow().setNavigationBarColor(Color.WHITE);
        try {String saved=getPreferences(0).getString("accountProfile","");if(!saved.isEmpty()){profile=new JSONObject(saved);OnlineClient.session=getPreferences(0).getString("accountSession","");raffles=new JSONArray(getPreferences(0).getString(accountKey(),"[]"));OnlineClient.backupRevision=getPreferences(0).getInt(accountKey()+":revision",0);}}
        catch (Exception e) { new AlertDialog.Builder(this).setMessage("Não foi possível ler os dados salvos.").setPositiveButton("OK",null).show(); }
        migratePayments();
        if(profile!=null){if(!profile.has("theme")){for(int i=0;i<themeColors.length;i++)if(green==themeColors[i]){try{profile.put("theme",themeIds[i]);getPreferences(0).edit().putString("accountProfile",profile.toString()).putBoolean(accountKey()+":themeDirty",true).commit();}catch(Exception ignored){}break;}}applyTheme();}
        OrderNotifications.schedule(this);
        if(profile==null)authScreen(false);else {home();askNotificationPermission();openOrderIntent(getIntent());}
    }
    boolean save() {
        if(profile==null)return false;
        try{for(int i=0;i<raffles.length();i++){JSONObject r=raffles.getJSONObject(i);r.put("accountId",profile.getString("id"));if(!r.has("id"))r.put("id",UUID.randomUUID().toString());}}catch(Exception e){return false;}
        if(getPreferences(0).edit().putString(accountKey(),raffles.toString()).putBoolean(accountKey()+":dirty",true).commit()) {editCounter++;onlineHandler.removeCallbacks(backupJob);onlineHandler.postDelayed(backupJob,2000);if(foreground&&!onlineBusy){onlineHandler.removeCallbacks(onlinePoll);onlineHandler.postDelayed(onlinePoll,1500);}return true;}
        toast("Não foi possível salvar. Tente novamente."); return false;
    }
    void migratePayments() {
        boolean changed=false;
        try {for(int i=0;i<raffles.length();i++) {
            JSONObject sales=raffles.optJSONObject(i).optJSONObject("sales");
            Iterator<String> keys=sales.keys();while(keys.hasNext()) {JSONObject sale=sales.optJSONObject(keys.next());if(!sale.has("paid")){sale.put("paid",true);changed=true;}}
        }if(changed)save();}catch(Exception e){toast("Não foi possível atualizar os pagamentos.");}
    }
    ArrayList<Integer> paidNumbers(JSONObject r) {
        Map<Integer,Boolean> states=new TreeMap<>();
        for(int n=1;n<=r.optInt("total");n++){JSONObject sale=r.optJSONObject("sales").optJSONObject(""+n);if(sale!=null)states.put(n,sale.optBoolean("paid",true));}
        return PaymentRules.eligible(states);
    }
    String paymentStatus(JSONObject r,Collection<Integer> nums) {
        int paid=0;for(int n:nums)if(r.optJSONObject("sales").optJSONObject(""+n).optBoolean("paid",true))paid++;
        return paid==nums.size()?"Pago":paid==0?"Não pago":"Parcial: "+paid+" pago(s), "+(nums.size()-paid)+" não pago(s)";
    }
    void buyerPayment(JSONObject r,String key) {
        ArrayList<Integer> nums=groups(r).get(key);if(nums==null)return;
        if(r.has("winner")){toast("Esta rifa já foi sorteada. Pagamentos encerrados.");return;}
        new AlertDialog.Builder(this).setTitle("Pagamento do comprador").setMessage("Alterar todos os "+nums.size()+" números deste comprador?\nAtual: "+paymentStatus(r,nums))
            .setNegativeButton("Cancelar",null).setNeutralButton("Não pago",(d,w)->setBuyerPayment(r,nums,false)).setPositiveButton("Pago",(d,w)->setBuyerPayment(r,nums,true)).show();
    }
    void setBuyerPayment(JSONObject r,ArrayList<Integer> nums,boolean paid) {
        if(r.has("winner"))return;
        ArrayList<Boolean> old=new ArrayList<>();
        try {for(int n:nums){JSONObject sale=r.optJSONObject("sales").optJSONObject(""+n);old.add(sale.optBoolean("paid",true));sale.put("paid",paid);}
            if(save())buyers(r);else for(int i=0;i<old.size();i++)r.optJSONObject("sales").optJSONObject(""+nums.get(i)).put("paid",old.get(i));
        }catch(Exception e){toast("Não foi possível alterar o pagamento.");}
    }
    void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    GradientDrawable bg(int color) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(14)); return d; }
    LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    void screen(String title, boolean back) {
        viewingRequests=false;visualSettingsOpen=false;
        LinearLayout root=column(); root.setBackgroundColor(Color.rgb(245,248,246));
        root.setOnApplyWindowInsetsListener((v,insets)-> { v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom()); return insets; });
        TextView header=label((back?"‹  ":"")+title,20); header.setTextColor(accentText()); header.setPadding(dp(12),dp(10),dp(12),dp(10)); header.setBackgroundColor(green);
        if(back) header.setOnClickListener(v->home()); root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); body=column(); body.setPadding(dp(10),dp(8),dp(10),dp(16)); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }
    TextView label(String text,int size) { TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(ink); t.setPadding(0,dp(4),0,dp(4)); return t; }
    Button button(String title, LinearLayout parent, Runnable action) { Button b=new Button(this); b.setText(title); b.setTextSize(14); b.setAllCaps(false); b.setMinHeight(0); b.setMinimumHeight(0); b.setPadding(dp(6),0,dp(6),0); b.setTextColor(accentText()); b.setBackground(bg(green)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(42)); p.setMargins(0,dp(4),0,dp(4)); parent.addView(b,p); b.setOnClickListener(v->action.run()); return b; }
    EditText field(LinearLayout l,String hint,String value,int type) { EditText e=new EditText(this); e.setHint(hint); e.setText(value); e.setInputType(type); l.addView(e,new LinearLayout.LayoutParams(-1,-2)); return e; }
    int sold(JSONObject r) { return r.optJSONObject("sales").length(); }
    String number(int n) { return String.format(Locale.US,"%03d",n); }
    void home() {
        current=null; screen("Minhas Rifas",false);
        if(profile==null){authScreen(false);return;}
        body.addView(label(profile.optString("name")+" • "+profile.optString("email"),13));
        button("Minha conta / Pix / sincronizar",body,()->accountMenu());
        body.addView(label("Organize suas rifas, número por número.",16));
        button("+ Adicionar nova rifa",body,()->edit(null));
        button("Configurar pedidos pela internet",body,()->onlineSettings());
        button("Configurações / cores / notificações",body,()->visualSettings());
        if(raffles.length()==0) body.addView(label("Sua primeira rifa começa aqui. Adicione um nome, uma imagem e a quantidade de números.",18));
        for(int i=0;i<raffles.length();i++) { final JSONObject r=raffles.optJSONObject(i); LinearLayout card=column(); card.setPadding(dp(16),dp(12),dp(16),dp(12)); card.setBackground(bg(Color.WHITE)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(12),0,0); body.addView(card,p); card.addView(label(r.optString("name"),21)); card.addView(label(sold(r)+" de "+r.optInt("total")+" vendidos • "+(r.has("winner")?"Sorteada":"Em andamento"),14)); button("Abrir rifa",card,()->detail(r)); }
    }
    void showImage(ImageView view,String path) {
        view.setMaxHeight(path==null||path.isEmpty()?dp(110):Integer.MAX_VALUE);
        if(path==null || path.isEmpty()) { view.setImageResource(br.com.minhasrifas.R.drawable.icon); return; }
        Bitmap b=BitmapFactory.decodeFile(path); if(b!=null)view.setImageBitmap(b); else view.setImageResource(br.com.minhasrifas.R.drawable.icon);
    }
    void edit(JSONObject r) {
        pendingImage=r==null?"":r.optString("image"); LinearLayout form=column(); form.setPadding(dp(18),dp(8),dp(18),dp(8));
        EditText name=field(form,"Nome da rifa",r==null?"":r.optString("name"),1);
        EditText desc=field(form,"Descrição / prêmio",r==null?"":r.optString("description"),android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText total=field(form,"Quantidade de números (1 a 10.000)",r==null?"100":""+r.optInt("total"),2); if(r!=null) total.setEnabled(false);
        imagePreview=new ImageView(this); imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER); imagePreview.setAdjustViewBounds(true); form.addView(imagePreview,new LinearLayout.LayoutParams(-1,-2)); showImage(imagePreview,pendingImage);
        button("Escolher imagem do celular",form,()->{ Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.setType("image/*"); intent.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(intent,10); });
        ScrollView sc=new ScrollView(this); sc.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(r==null?"Nova rifa":"Editar rifa").setView(sc).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
            int count; try { count=Integer.parseInt(total.getText().toString()); } catch(Exception e) {total.setError("Informe uma quantidade");return;}
            if(name.getText().toString().trim().isEmpty()) {name.setError("Informe o nome");return;} if(count<1||count>10000) {total.setError("Use de 1 a 10.000 números");return;}
            try { JSONObject dest=r==null?new JSONObject():r; dest.put("name",name.getText().toString().trim()).put("description",desc.getText().toString().trim()).put("total",count).put("image",pendingImage); if(r==null) {dest.put("sales",new JSONObject()); raffles.put(dest);} if(save()) {dialog.dismiss(); detail(dest);} }catch(Exception e){toast("Não foi possível salvar a rifa.");}
        })); dialog.show();
    }
    void detail(JSONObject r) {
        current=r; screen(r.optString("name"),true);
        ImageView image=new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setAdjustViewBounds(true); image.setBackground(bg(Color.rgb(221,235,226))); body.addView(image,new LinearLayout.LayoutParams(-1,-2)); showImage(image,r.optString("image"));
        Button draw=button(r.has("winner")?"Ver resultado do sorteio":"Sortear rifa",body,()->draw(r));
        boolean ready=sold(r)==r.optInt("total"); draw.setEnabled((ready&&!paidNumbers(r).isEmpty())||r.has("winner")); draw.setAlpha(draw.isEnabled()?1f:.45f);
        if(!r.optString("description").isEmpty()) body.addView(label(r.optString("description"),14));
        body.addView(label(sold(r)+" vendidos  •  "+(r.optInt("total")-sold(r))+" disponíveis",14));
        body.addView(label(paidNumbers(r).size()+" pagos • "+(sold(r)-paidNumbers(r).size())+" não pagos. Só números pagos participam.",12));
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(r.optInt("total")); progress.setProgress(sold(r)); body.addView(progress);
        if(!ready)body.addView(label("Sorteio disponível ao vender todos os números.",12));
        LinearLayout actions=new LinearLayout(this); body.addView(actions);
        Button list=button("Compradores",actions,()->buyers(r)), editButton=button("Editar rifa",actions,()->edit(r));
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(40),1); left.setMargins(0,dp(4),dp(4),dp(4)); list.setLayoutParams(left);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(40),1); right.setMargins(dp(4),dp(4),0,dp(4)); editButton.setLayoutParams(right);
        button(r.has("webId")?"Link e pedidos online ("+pendingCount(r)+")":"Gerar link para pedidos",body,()->onlineMenu(r));
        button("Pix / copiar chave",body,()->new AlertDialog.Builder(this).setTitle("Pagamento por Pix").setMessage(PixInfo.instructions(profile.optString("pixName"),profile.optString("pixKey"))).setNegativeButton("Fechar",null).setPositiveButton("Copiar chave",(d,w)->{if(profile.optString("pixKey").isEmpty()){toast("Configure seu Pix em Minha conta.");return;}android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("Chave Pix",profile.optString("pixKey")));toast("Chave Pix copiada.");}).show());
        if(!r.has("winner") && !ready) button("+ Comprador / vários números",body,()->bulkSale(r));
        body.addView(label("Números • Cor do tema = vendido",14));
        GridLayout grid=new GridLayout(this); body.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        int available=getResources().getDisplayMetrics().widthPixels-dp(20);
        int columns=Math.max(4,Math.min(8,available/dp(46))); grid.setColumnCount(columns);
        int width=available/columns;
        for(int n=1;n<=r.optInt("total");n++) { final int num=n; boolean taken=r.optJSONObject("sales").has(""+n); TextView t=label(number(n),12); t.setPadding(0,0,0,0); t.setGravity(Gravity.CENTER); t.setTextColor(taken?accentText():ink); t.setBackground(bg(taken?green:Color.WHITE)); t.setContentDescription("Número "+n+(taken?", vendido":", disponível")); GridLayout.LayoutParams p=new GridLayout.LayoutParams(); p.width=width-dp(4); p.height=dp(38); p.setMargins(dp(2),dp(2),dp(2),dp(2)); grid.addView(t,p); t.setOnClickListener(v->sale(r,num)); }
    }
    void sale(JSONObject r,int n) {
        JSONObject sales=r.optJSONObject("sales"), old=sales.optJSONObject(""+n);
        LinearLayout form=column(); form.setPadding(dp(20),dp(8),dp(20),dp(8));
        EditText name=field(form,"Nome do comprador",old==null?"":old.optString("name"),1);
        EditText phone=field(form,"Telefone com DDD (opcional)",old==null?"":old.optString("phone"),3);
        CheckBox paid=new CheckBox(this);paid.setText("Pago");paid.setChecked(old==null||old.optBoolean("paid",true));paid.setEnabled(!r.has("winner"));form.addView(paid);
        if(old!=null) button("Comprovante deste comprador",form,()->receipt(r,ReceiptSupport.buyerKey(old.optString("name"),old.optString("phone"))));
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle("Número "+number(n)).setView(form).setNegativeButton("Fechar",null);
        if(!r.has("winner")) {builder.setPositiveButton("Salvar venda",null); if(old!=null)builder.setNeutralButton("Liberar número",(d,w)->new AlertDialog.Builder(this).setMessage("Remover esta venda e liberar o número "+number(n)+"?").setNegativeButton("Cancelar",null).setPositiveButton("Liberar",(a,b)->{sales.remove(""+n);if(save())detail(r);}).show());}
        else {name.setEnabled(false); phone.setEnabled(false);}
        AlertDialog dialog=builder.create(); dialog.setOnShowListener(x->{if(r.has("winner"))return; dialog.getButton(-1).setOnClickListener(v->{
            String buyer=name.getText().toString().trim(), tel=phone.getText().toString().trim();
            if(buyer.isEmpty()){name.setError("Informe o nome");return;} if(!tel.isEmpty() && tel.replaceAll("\\D", "").length()<8){phone.setError("Informe um telefone válido ou deixe em branco");return;}
            try {sales.put(""+n,new JSONObject().put("name",buyer).put("phone",tel).put("paid",paid.isChecked())); if(save()){dialog.dismiss();detail(r);}}catch(Exception e){toast("Erro ao salvar venda.");}
        });}); dialog.show();
    }
    void draw(JSONObject r) {
        if(r.has("winner")){result(r);return;} if(sold(r)!=r.optInt("total"))return;
        ArrayList<Integer> eligible=paidNumbers(r);if(eligible.isEmpty()){toast("Nenhum número pago para sortear.");return;}
        new AlertDialog.Builder(this).setTitle("Realizar sorteio?").setMessage("Participarão apenas os "+eligible.size()+" números pagos. Os "+(sold(r)-eligible.size())+" números não pagos ficam fora. O resultado ficará salvo, e os pagamentos serão encerrados.").setNegativeButton("Cancelar",null).setPositiveButton("Sortear",(d,w)->{
            try {ArrayList<Integer> confirmed=paidNumbers(r);if(confirmed.isEmpty()){toast("Nenhum número pago para sortear.");return;}r.put("winner",PaymentRules.pick(confirmed,new SecureRandom().nextInt(confirmed.size()))); r.put("drawnAt",new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date())); if(save()){detail(r);result(r);} else {r.remove("winner");r.remove("drawnAt");}}catch(Exception e){toast("Erro ao sortear.");}
        }).show();
    }
    void result(JSONObject r) {JSONObject buyer=r.optJSONObject("sales").optJSONObject(""+r.optInt("winner")); new AlertDialog.Builder(this).setTitle("Número vencedor: "+number(r.optInt("winner"))).setMessage(buyer.optString("name")+"\n"+buyer.optString("phone")+"\n\nSorteio: "+r.optString("drawnAt")).setPositiveButton("OK",null).show();}
    void buyers(JSONObject r) {
        screen("Compradores",true); current=r;
        body.addView(label(r.optString("name"),18));
        body.addView(label(sold(r)+" de "+r.optInt("total")+" números vendidos",13));
        Switch phones=new Switch(this); phones.setText("Mostrar telefones"); phones.setTextSize(14);
        phones.setChecked(getPreferences(0).getBoolean("showBuyerPhones",false)); body.addView(phones);
        ArrayList<TextView> phoneRows=new ArrayList<>();
        phones.setOnCheckedChangeListener((view,show)->{
            getPreferences(0).edit().putBoolean("showBuyerPhones",show).apply();
            for(TextView row:phoneRows)row.setVisibility(show?View.VISIBLE:View.GONE);
        });
        if(sold(r)==0)body.addView(label("Nenhum comprador cadastrado ainda.",17));
        for(Map.Entry<String,ArrayList<Integer>> entry:groups(r).entrySet()) {
            String key=entry.getKey(); ArrayList<Integer> nums=entry.getValue(); JSONObject buyer=r.optJSONObject("sales").optJSONObject(""+nums.get(0));
            LinearLayout row=column(); row.setPadding(0,dp(6),0,dp(6)); body.addView(row);
            row.addView(label(buyer.optString("name")+" — "+ReceiptSupport.numbers(nums),15));
            TextView status=label(paymentStatus(r,nums)+(r.has("winner")?"":" • tocar para alterar"),12);status.setTextColor(paymentStatus(r,nums).equals("Pago")?(green==themeColors[3]?ink:green):Color.rgb(166,75,20));row.addView(status);status.setOnClickListener(v->buyerPayment(r,key));
            if(!buyer.optString("phone").isEmpty()) {
                TextView phone=label(buyer.optString("phone"),13); phone.setVisibility(phones.isChecked()?View.VISIBLE:View.GONE);
                phoneRows.add(phone); row.addView(phone);
            }
            row.setOnClickListener(v->receipt(r,key));
            if(!r.has("winner"))button("Excluir cliente / liberar números",row,()->deleteBuyer(r,key));
            View divider=new View(this); divider.setBackgroundColor(Color.rgb(218,228,222)); body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        body.addView(label("Toque no comprador para ver ou compartilhar o comprovante.",12));
        if(!r.has("winner") && sold(r)<r.optInt("total")) button("+ Comprador / vários números",body,()->bulkSale(r));
        button("Exportar lista (CSV)",body,()->{StringBuilder csv=new StringBuilder("\uFEFFNúmero;Nome;Telefone\r\n"); JSONObject sales=r.optJSONObject("sales");for(int n=1;n<=r.optInt("total");n++){JSONObject b=sales.optJSONObject(""+n);if(b!=null)csv.append(number(n)).append(';').append(csvCell(b.optString("name"))).append(';').append(csvCell(b.optString("phone"))).append("\r\n");} exportText=csv.toString(); Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"compradores.csv");startActivityForResult(i,11);});
        button("Voltar à rifa",body,()->detail(r));
    }
    void deleteBuyer(JSONObject r,String key){
        if(onlineBusy||accountBusy){toast("Aguarde a sincronização atual.");return;}
        if(r.has("winner")){toast("Rifa já sorteada.");return;}
        ArrayList<Integer> nums=groups(r).get(key);if(nums==null)return;
        boolean linked=false;for(int n:nums)if(!r.optJSONObject("sales").optJSONObject(""+n).optString("requestId").isEmpty())linked=true;
        if(linked)managementRefresh(r,()->buyerDeleteDialog(r,key));else buyerDeleteDialog(r,key);
    }
    void buyerDeleteDialog(JSONObject r,String key){
        ArrayList<Integer> selected=groups(r).get(key);if(selected==null){buyers(r);return;}
        JSONObject buyer=r.optJSONObject("sales").optJSONObject(""+selected.get(0));final int revision=r.optInt("webRevision",-1);
        new AlertDialog.Builder(this).setTitle("Excluir cliente?").setMessage(buyer.optString("name")+"\nNúmeros: "+ReceiptSupport.numbers(selected)+"\n\nRemover este cliente desta rifa e liberar seus números? Pedidos online vinculados serão cancelados. Os demais compradores serão preservados.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Excluir e liberar",(d,w)->deleteBuyerConfirmed(r,key,selected,revision)).show();
    }
    void deleteBuyerConfirmed(JSONObject r,String key,ArrayList<Integer> selected,int revision){
        if(onlineBusy||accountBusy){toast("Aguarde a sincronização atual.");return;}if(r.has("winner"))return;
        boolean liveRaffle=false;for(int i=0;i<raffles.length();i++)if(raffles.optJSONObject(i)==r)liveRaffle=true;if(!liveRaffle){toast("Dados atualizados. Abra novamente a rifa para conferir o cliente.");home();return;}
        ArrayList<Integer> currentNumbers=groups(r).get(key);if(currentNumbers==null||!currentNumbers.equals(selected)){toast("Os números deste cliente mudaram. Confira novamente.");buyers(r);return;}
        try{JSONObject sales=r.getJSONObject("sales");Set<String> requestIds=new LinkedHashSet<>(),selectedKeys=new HashSet<>();for(int n:selected){selectedKeys.add(""+n);String id=sales.getJSONObject(""+n).optString("requestId");if(!id.isEmpty())requestIds.add(id);}
            if(requestIds.isEmpty()){JSONObject original=new JSONObject(sales.toString());for(int n:selected)sales.remove(""+n);if(save()){buyers(r);toast("Cliente excluído. Números liberados.");if(r.has("webId"))syncOnline(r,true,()->buyers(r));}else r.put("sales",original);return;}
            JSONArray preserve=new JSONArray();Iterator<String> keys=sales.keys();while(keys.hasNext()){String n=keys.next();if(!selectedKeys.contains(n))preserve.put(Integer.parseInt(n));}
            JSONObject data=new JSONObject().put("preserve",preserve);onlineBusy=true;managingOrders=true;
            onlineTask(()->{
                JSONObject last=null;int expected=revision;for(String id:requestIds){try{last=OnlineClient.call(r.optString("webBase"),"/api/raffles/"+r.optString("webId")+"/requests/"+id+"/delete","POST",r.optString("webToken"),expected,data);expected=last.getJSONObject("raffle").getInt("revision");}catch(Exception e){if(last==null)throw e;last.put("buyerDeleteError",e.getMessage());return last;}}return last;
            },response->{
                try{Set<String> cancelled=new HashSet<>();JSONArray ids=response.optJSONArray("cancelled");if(ids!=null)for(int i=0;i<ids.length();i++)cancelled.add(ids.optString(i));Map<String,String> owners=new LinkedHashMap<>();JSONObject live=r.getJSONObject("sales");Iterator<String> it=live.keys();while(it.hasNext()){String n=it.next();owners.put(n,live.getJSONObject(n).optString("requestId"));}
                    // A shared order may have numbers reassigned to another buyer. Keep their sales.
                    Set<String> sharedCancelled=new HashSet<>(cancelled);sharedCancelled.retainAll(requestIds);for(String n:RequestNumbers.detachedKeys(owners,sharedCancelled,selectedKeys))live.getJSONObject(n).remove("requestId");
                    boolean complete=!response.has("buyerDeleteError");if(complete)for(int n:selected)live.remove(""+n);
                    boolean applied=importRequests(r,response);onlineBusy=false;managingOrders=false;buyers(r);
                    if(complete&&applied){toast("Cliente excluído. Números liberados.");onlineBusy=true;publishSnapshot(r,"/api/raffles/"+r.optString("webId"),r.optString("webBase"),r.optString("webToken"),true,()->{buyers(r);accountSync(false);});}
                    else if(!complete)toast("Parte dos pedidos foi cancelada. Confira os números restantes e tente novamente. "+response.optString("buyerDeleteError"));
                }catch(Exception e){onlineBusy=false;managingOrders=false;toast("Confira os compradores e atualize os pedidos antes de tentar novamente.");}
            },()->managingOrders=false,true);
        }catch(Exception e){onlineBusy=false;managingOrders=false;toast("Não foi possível excluir o cliente.");}
    }
    LinkedHashMap<String,ArrayList<Integer>> groups(JSONObject r) {
        LinkedHashMap<String,ArrayList<Integer>> groups=new LinkedHashMap<>();
        for(int n=1;n<=r.optInt("total");n++) {
            JSONObject b=r.optJSONObject("sales").optJSONObject(""+n); if(b==null)continue;
            String key=ReceiptSupport.buyerKey(b.optString("name"),b.optString("phone"));
            if(!groups.containsKey(key))groups.put(key,new ArrayList<Integer>());
            groups.get(key).add(n);
        }
        return groups;
    }
    void bulkSale(JSONObject r) {
        if(r.has("winner"))return;
        LinearLayout form=column(); form.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText name=field(form,"Nome do comprador","",1), phone=field(form,"Telefone com DDD (opcional)","",3);
        CheckBox paid=new CheckBox(this);paid.setText("Pago");paid.setChecked(true);form.addView(paid);
        LinkedHashMap<String,ArrayList<Integer>> existing=groups(r);
        if(!existing.isEmpty()) button("Escolher comprador já cadastrado",form,()->{
            ArrayList<String> keys=new ArrayList<>(existing.keySet()); String[] labels=new String[keys.size()];
            for(int i=0;i<keys.size();i++){JSONObject b=r.optJSONObject("sales").optJSONObject(""+existing.get(keys.get(i)).get(0));labels[i]=b.optString("name")+(b.optString("phone").isEmpty()?"":" • "+b.optString("phone"));}
            new AlertDialog.Builder(this).setTitle("Selecionar comprador").setItems(labels,(d,pos)->{JSONObject b=r.optJSONObject("sales").optJSONObject(""+existing.get(keys.get(pos)).get(0));name.setText(b.optString("name"));phone.setText(b.optString("phone"));}).setNegativeButton("Cancelar",null).show();
        });
        TextView summary=label("Nenhum número selecionado",14); form.addView(summary);
        TreeSet<Integer> selected=new TreeSet<>();
        button("Escolher números disponíveis",form,()->{
            ArrayList<Integer> available=new ArrayList<>(); for(int n=1;n<=r.optInt("total");n++)if(!r.optJSONObject("sales").has(""+n))available.add(n);
            String[] labels=new String[available.size()]; boolean[] checked=new boolean[available.size()]; TreeSet<Integer> draft=new TreeSet<>(selected);
            for(int i=0;i<labels.length;i++){labels[i]="Número "+number(available.get(i));checked[i]=draft.contains(available.get(i));}
            new AlertDialog.Builder(this).setTitle("Selecionar números").setMultiChoiceItems(labels,checked,(d,pos,on)->{if(on)draft.add(available.get(pos));else draft.remove(available.get(pos));})
                .setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->{selected.clear();selected.addAll(draft);summary.setText(selected.isEmpty()?"Nenhum número selecionado":selected.size()+" selecionado(s): "+ReceiptSupport.numbers(selected));}).show();
        });
        form.addView(label("O mesmo nome e telefone agrupam os números do comprador nesta rifa. Para pessoas com o mesmo nome, use um sobrenome ou outra identificação.",12));
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Comprador e números").setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Salvar e gerar comprovante",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            String buyer=name.getText().toString().trim(), tel=phone.getText().toString().trim();
            if(buyer.isEmpty()){name.setError("Informe o nome");return;}
            if(!tel.isEmpty() && tel.replaceAll("\\D", "").length()<8){phone.setError("Informe um telefone válido ou deixe em branco");return;}
            if(selected.isEmpty()){toast("Selecione ao menos um número.");return;}
            if(r.has("winner")){toast("Esta rifa já foi sorteada.");return;}
            JSONObject sales=r.optJSONObject("sales");for(int n:selected)if(sales.has(""+n)){toast("Um número selecionado já foi vendido. Selecione novamente.");return;}
            try {
                for(int n:selected)sales.put(""+n,new JSONObject().put("name",buyer).put("phone",tel).put("paid",paid.isChecked()));
                if(save()){dialog.dismiss();detail(r);receipt(r,ReceiptSupport.buyerKey(buyer,tel));}
                else for(int n:selected)sales.remove(""+n);
            }catch(Exception e){for(int n:selected)sales.remove(""+n);toast("Não foi possível salvar a compra.");}
        }));dialog.show();
    }
    void receipt(JSONObject r,String key) {
        ArrayList<Integer> nums=groups(r).get(key);if(nums==null||nums.isEmpty()){toast("Nenhum número encontrado para este comprador.");return;}
        JSONObject buyer=r.optJSONObject("sales").optJSONObject(""+nums.get(0));
        ArrayList<Integer> paidList=new ArrayList<>();for(int n:nums){if(r.optJSONObject("sales").optJSONObject(""+n).optBoolean("paid",true))paidList.add(n);}
        String text=ReceiptSupport.receipt(r.optString("name"),r.optString("description"),buyer.optString("name"),buyer.optString("phone"),nums,new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date()),profile.optString("pixName"),profile.optString("pixKey"))+"\n\nPagamento: "+paymentStatus(r,nums)+"\nNúmeros pagos: "+(paidList.isEmpty()?"Nenhum":ReceiptSupport.numbers(paidList))+"\nSomente os números pagos participam do sorteio.\n\nObrigado Deus abençoe!";
        TextView content=label(text,15);content.setPadding(dp(18),dp(8),dp(18),dp(8));content.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(content);
        new AlertDialog.Builder(this).setTitle("Comprovante").setView(scroll).setNegativeButton("Fechar",null).setPositiveButton("Compartilhar",(d,w)->{
            Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_SUBJECT,"Comprovante • "+r.optString("name"));share.putExtra(Intent.EXTRA_TEXT,text);
            try {startActivity(Intent.createChooser(share,"Enviar comprovante pelo WhatsApp ou outro app"));}catch(ActivityNotFoundException e){toast("Nenhum aplicativo disponível para compartilhar.");}
        }).show();
    }
    String accountKey(){return "rifas:"+profile.optString("id");}
    void authScreen(boolean register) {
        current=null;screen(register?"Criar conta por convite":"Entrar na sua conta",false);
        body.addView(label("Cada conta tem suas próprias rifas e chave Pix. Para criar conta, use o convite recebido junto com o APK.",14));
        EditText name=register?field(body,"Seu nome","",1):null;
        EditText email=field(body,"E-mail","",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password=field(body,"Senha (mínimo 12 caracteres)","",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText invite=register?field(body,"Código de convite","",1):null;
        button(register?"Criar minha conta":"Entrar",body,()->{
            if(accountBusy||onlineBusy){toast("Aguarde a conexão atual.");return;}
            if(password.getText().length()<12){password.setError("Use pelo menos 12 caracteres");return;}
            try {JSONObject data=new JSONObject().put("email",email.getText().toString().trim()).put("password",password.getText().toString());if(register)data.put("name",name.getText().toString().trim()).put("invite",invite.getText().toString().trim());
                accountTask(()->OnlineClient.call(OnlineClient.DEFAULT_BASE,register?"/api/auth/register":"/api/auth/login","POST",null,-1,data),response->{try{profile=response.getJSONObject("user");OnlineClient.session=response.getString("token");getPreferences(0).edit().putString("accountProfile",profile.toString()).putString("accountSession",OnlineClient.session).commit();raffles=new JSONArray(getPreferences(0).getString(accountKey(),"[]"));OnlineClient.backupRevision=getPreferences(0).getInt(accountKey()+":revision",0);password.setText("");applyTheme();home();OrderNotifications.schedule(this);askNotificationPermission();accountSync(true);}catch(Exception e){toast("Não foi possível abrir a conta.");}},true);
            }catch(Exception e){toast("Confira os dados.");}
        });
        button(register?"Já tenho conta":"Criar conta por convite",body,()->authScreen(!register));
        body.addView(label("Administrador: ative sua conta pelo painel web antes de entrar. Os dados da versão anterior permanecem neste aparelho, aguardando importação para sua conta.",12));
    }
    void accountTask(OnlineWork work,OnlineResult result,boolean visible) {
        if(accountBusy){if(visible)toast("Aguarde a sincronização da conta.");return;}accountBusy=true;
        new Thread(()->{try{JSONObject response=work.run();runOnUiThread(()->{accountBusy=false;result.done(response);});}catch(Exception e){runOnUiThread(()->{accountBusy=false;if(visible)toast(e.getMessage()==null?"Não foi possível conectar.":e.getMessage());});}}).start();
    }
    void accountMenu() {
        ArrayList<String> options=new ArrayList<>(Arrays.asList("Sincronizar meus dados","Configurar meu Pix","Restaurar dados da nuvem","Entrar novamente"));
        boolean admin="admin".equals(profile.optString("role"));if(admin){options.add("Importar rifas da versão anterior");options.add("Painel de convites e usuários");}options.add("Sair da conta");
        new AlertDialog.Builder(this).setTitle(profile.optString("email")).setItems(options.toArray(new String[0]),(d,index)->{
            String action=options.get(index);if(action.startsWith("Sincronizar"))accountSync(true);else if(action.startsWith("Configurar"))accountPix();else if(action.startsWith("Restaurar"))new AlertDialog.Builder(this).setMessage("Substituir os dados desta conta pelos da nuvem? Uma cópia local das alterações atuais será preservada neste aparelho.").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar",(a,b)->restoreBackup(true)).show();else if(action.startsWith("Entrar"))authScreen(false);else if(action.startsWith("Importar"))importLegacy();else if(action.startsWith("Painel"))startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(OnlineClient.DEFAULT_BASE+"/admin.html")));else logoutAccount();
        }).show();
    }
    void accountPix() {
        LinearLayout form=column();form.setPadding(dp(16),dp(8),dp(16),dp(8));EditText name=field(form,"Nome do destinatário",profile.optString("pixName"),1),key=field(form,"Sua chave Pix",profile.optString("pixKey"),1);
        new AlertDialog.Builder(this).setTitle("Pix da minha conta").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{try {JSONObject data=new JSONObject().put("name",name.getText().toString().trim()).put("key",key.getText().toString().trim());accountTask(()->OnlineClient.call(OnlineClient.DEFAULT_BASE,"/api/account/pix","PUT",OnlineClient.session,-1,data),response->{try{profile=response.getJSONObject("user");getPreferences(0).edit().putString("accountProfile",profile.toString()).commit();toast("Pix da sua conta atualizado.");onlineHandler.removeCallbacks(onlinePoll);onlineHandler.postDelayed(onlinePoll,1000);}catch(Exception e){toast("Erro ao salvar Pix.");}},true);}catch(Exception e){toast("Confira os dados.");}}).show();
    }
    JSONArray backupSnapshot()throws Exception {
        JSONArray snapshot=new JSONArray(raffles.toString());for(int i=0;i<snapshot.length();i++){JSONObject copy=snapshot.getJSONObject(i),original=raffles.getJSONObject(i);String data=publicSnapshot(original).optString("image");if(data.isEmpty())data=original.optString("imageData");copy.put("imageData",data);copy.remove("image");}return snapshot;
    }
    void accountSync(boolean visible) {
        if(profile==null||accountBusy||onlineBusy||managingOrders||OnlineClient.session.isEmpty())return;
        final String scope=accountKey();final long counter=editCounter;
        if(!getPreferences(0).getBoolean(scope+":dirty",false)){restoreBackup(false);return;}
        try {JSONObject data=new JSONObject().put("revision",getPreferences(0).getInt(scope+":revision",0)).put("raffles",backupSnapshot());
            accountTask(()->OnlineClient.call(OnlineClient.DEFAULT_BASE,"/api/account/backup","PUT",OnlineClient.session,-1,data),response->{if(profile==null||!scope.equals(accountKey()))return;OnlineClient.backupRevision=response.optInt("revision");getPreferences(0).edit().putInt(scope+":revision",response.optInt("revision")).putBoolean(scope+":dirty",counter!=editCounter).commit();if(visible)toast("Dados da sua conta salvos online.");},visible);
        }catch(Exception e){if(visible)toast("Não foi possível preparar o backup. Os dados locais foram preservados.");}
    }
    void restoreBackup(boolean force) {
        if(profile==null||accountBusy||onlineBusy||managingOrders)return;final String scope=accountKey();final long counter=editCounter;
        accountTask(()->OnlineClient.call(OnlineClient.DEFAULT_BASE,"/api/account/backup","GET",OnlineClient.session,-1,null),response->{
            if(profile==null||!scope.equals(accountKey())||counter!=editCounter){if(force)toast("Há alterações locais recentes. Sincronize novamente.");return;}
            int revision=response.optInt("revision");if(!force&&revision==getPreferences(0).getInt(scope+":revision",0)){OnlineClient.backupRevision=revision;return;}
            try {JSONArray restored=response.getJSONArray("raffles");for(int i=0;i<restored.length();i++){JSONObject r=restored.getJSONObject(i);String data=r.optString("imageData");if(!data.isEmpty()){byte[] bytes=android.util.Base64.decode(data.substring(data.indexOf(',')+1),android.util.Base64.DEFAULT);File f=new File(getFilesDir(),"restaurada-"+UUID.randomUUID()+".jpg");try(OutputStream out=new FileOutputStream(f)){out.write(bytes);}r.put("image",f.getAbsolutePath());}}
                android.content.SharedPreferences.Editor prefs=getPreferences(0).edit();if(force)prefs.putString(scope+":antes-restauracao",raffles.toString());if(!prefs.putString(scope,restored.toString()).putInt(scope+":revision",revision).putBoolean(scope+":dirty",false).commit()){toast("Não foi possível restaurar; dados locais preservados.");return;}raffles=restored;OnlineClient.backupRevision=revision;home();if(force)toast("Dados da nuvem restaurados.");
            }catch(Exception e){toast("Não foi possível restaurar. Seus dados locais foram preservados.");}
        },true);
    }
    void importLegacy() {
        if(!"admin".equals(profile.optString("role")))return;
        new AlertDialog.Builder(this).setTitle("Importar rifas antigas").setMessage("Copiar as rifas da versão anterior deste aparelho para sua conta? Os dados originais serão mantidos.").setNegativeButton("Cancelar",null).setPositiveButton("Importar",(d,w)->{
            try {JSONArray legacy=new JSONArray(getPreferences(0).getString("rifas","[]"));Set<String> ids=new HashSet<>();for(int i=0;i<raffles.length();i++)ids.add(raffles.getJSONObject(i).optString("id"));for(int i=0;i<legacy.length();i++)if(!legacy.getJSONObject(i).has("id"))legacy.getJSONObject(i).put("id",UUID.randomUUID().toString());if(!getPreferences(0).edit().putString("rifas",legacy.toString()).commit()){toast("Não foi possível preparar a importação.");return;}
                int added=0;for(int i=0;i<legacy.length();i++){JSONObject r=legacy.getJSONObject(i);if(!ids.contains(r.getString("id"))){r.put("accountId",profile.getString("id"));raffles.put(r);added++;}}if(save()){migratePayments();home();toast(added+" rifa(s) importada(s). Sincronize para salvar online.");accountSync(true);}
            }catch(Exception e){toast("Falha na importação. Rifas originais preservadas.");}
        }).show();
    }
    void logoutAccount() {
        if(onlineBusy||accountBusy){toast("Aguarde a sincronização terminar.");return;}
        if(getPreferences(0).getBoolean(accountKey()+":dirty",false)){toast("Há dados pendentes de sincronização. Sincronize antes de sair.");return;}
        final String token=OnlineClient.session;new Thread(()->{try{OnlineClient.call(OnlineClient.DEFAULT_BASE,"/api/auth/logout","POST",token,-1,new JSONObject());}catch(Exception e){}}).start();
        getPreferences(0).edit().remove("accountProfile").remove("accountSession").commit();OrderNotifications.clear(this);OnlineClient.session="";OnlineClient.backupRevision=0;profile=null;green=themeColors[0];getWindow().setStatusBarColor(green);raffles=new JSONArray();current=null;onlineHandler.removeCallbacks(onlinePoll);onlineHandler.removeCallbacks(backupJob);authScreen(false);
    }
    String csvCell(String s) { if(s.matches("^[=+@-].*"))s="'"+s; return "\""+s.replace("\"","\"\"")+"\""; }
    void applyTheme(){
        green=themeColors[0];if(profile!=null)for(int i=0;i<themeIds.length;i++)if(themeIds[i].equals(profile.optString("theme"))){green=themeColors[i];break;}
        getWindow().setStatusBarColor(green);int flags=getWindow().getDecorView().getSystemUiVisibility();getWindow().getDecorView().setSystemUiVisibility(green==themeColors[3]?flags|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:flags&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    void syncTheme(boolean visible){
        if(profile==null||accountBusy||onlineBusy||managingOrders||OnlineClient.session.isEmpty())return;
        final String scope=accountKey();final String chosen=profile.optString("theme","green");boolean dirty=getPreferences(0).getBoolean(scope+":themeDirty",false);
        try{JSONObject data=dirty?new JSONObject().put("theme",chosen):null;
            accountTask(()->OnlineClient.call(OnlineClient.DEFAULT_BASE,"/api/account/theme",dirty?"PUT":"GET",OnlineClient.session,-1,data),response->{
                if(profile==null||!scope.equals(accountKey()))return;
                if(!chosen.equals(profile.optString("theme","green")))return;
                try{int old=green;profile=response.getJSONObject("user");getPreferences(0).edit().putString("accountProfile",profile.toString()).putBoolean(scope+":themeDirty",false).commit();applyTheme();if(old!=green){if(visualSettingsOpen)visualSettings();else if(current!=null){if(viewingRequests)requestsScreen(current);else detail(current);}else home();}if(visible)toast("Cor da sua conta salva online.");accountSync(false);}catch(Exception e){if(visible)toast("Cor local preservada; tente sincronizar novamente.");}
            },visible);
        }catch(Exception e){if(visible)toast("Cor local preservada; sincronize ao reconectar.");}
    }
    void askNotificationPermission() {
        if(!getPreferences(0).getBoolean("orderNotifications",true))return;
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},80);
    }
    void visualSettings() {
        current=null;screen("Configurações",true);visualSettingsOpen=true;
        body.addView(label("Cor dos botões, destaques e números vendidos",19));
        String[] names={"Verde","Vermelho","Rosa","Amarelo","Azul","Roxo"};
        int[] colors=themeColors;
        for(int i=0;i<names.length;i++){final int color=colors[i];final String theme=themeIds[i];Button b=button(names[i]+(green==color?" ✓":""),body,()->{try{profile.put("theme",theme);getPreferences(0).edit().putString("accountProfile",profile.toString()).putBoolean(accountKey()+":themeDirty",true).commit();applyTheme();visualSettings();syncTheme(true);}catch(Exception e){toast("Não foi possível salvar a cor.");}});b.setBackground(bg(color));b.setTextColor(color==colors[3]?ink:Color.WHITE);}
        body.addView(label("A cor vale para toda a sua conta, inclusive os números vendidos e a página pública. Alterações sem internet serão enviadas ao reconectar.",13));
        Switch enabled=new Switch(this);enabled.setText("Avisar quando chegar novo pedido");enabled.setChecked(getPreferences(0).getBoolean("orderNotifications",true));body.addView(enabled);
        enabled.setOnCheckedChangeListener((b,on)->{getPreferences(0).edit().putBoolean("orderNotifications",on).commit();if(on){OrderNotifications.schedule(this);askNotificationPermission();}else OrderNotifications.clear(this);});
        body.addView(label("Com o app aberto, as consultas alternam entre pedidos das rifas e backup da conta. Em segundo plano, o Android agenda consultas a partir de 15 minutos e pode atrasá-las para economizar bateria. É preciso internet; forçar parada impede os avisos até abrir o app.",13));
        button("Permissão de notificações no Android",body,()->{askNotificationPermission();Intent intent=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(intent);});
    }
    void openOrderIntent(Intent intent){if(profile==null||intent==null)return;String id=intent.getStringExtra("orderRaffle");if(id==null)return;intent.removeExtra("orderRaffle");for(int i=0;i<raffles.length();i++){JSONObject r=raffles.optJSONObject(i);if(id.equals(r.optString("webId"))){requestsScreen(r);syncOnline(r,false,()->requestsScreen(r));return;}}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openOrderIntent(intent);}
    @Override protected void onResume(){super.onResume();foreground=true;onlineHandler.removeCallbacks(onlinePoll);onlineHandler.postDelayed(onlinePoll,1000);}
    @Override protected void onPause(){super.onPause();foreground=false;onlineHandler.removeCallbacks(onlinePoll);onlineHandler.removeCallbacks(backupJob);}
    interface OnlineWork { JSONObject run() throws Exception; }
    interface OnlineResult { void done(JSONObject value); }
    void onlineTask(OnlineWork work,OnlineResult result,Runnable failed,boolean visible) {
        new Thread(()->{try {JSONObject response=work.run();runOnUiThread(()->result.done(response));}
            catch(Exception e){runOnUiThread(()->{onlineBusy=false;if(visible)toast(e.getMessage()==null?"Falha de conexão. Tente novamente.":e.getMessage());if(failed!=null)failed.run();});}}).start();
    }
    void onlineSettings() {
        LinearLayout form=column();form.setPadding(dp(16),dp(8),dp(16),dp(8));
        form.addView(label("Depois de ativar o serviço gratuito no Cloudflare, cole o endereço HTTPS aqui. Rifas já publicadas mantêm sua hospedagem original.",14));
        EditText url=field(form,"https://seu-servico.workers.dev",getPreferences(0).getString("onlineBase",""),android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Hospedagem dos pedidos").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Verificar e salvar",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{if(onlineBusy){toast("Aguarde a sincronização atual.");return;}try {
            final String base=OnlineClient.base(url.getText().toString());onlineBusy=true;dialog.getButton(-1).setEnabled(false);
            onlineTask(()->OnlineClient.call(base,"/api/health","GET",null,-1,null),response->{onlineBusy=false;dialog.getButton(-1).setEnabled(true);if(!"minhas-rifas".equals(response.optString("service"))){toast("Este endereço não é do serviço Minhas Rifas.");return;}getPreferences(0).edit().putString("onlineBase",base).commit();dialog.dismiss();toast("Hospedagem configurada.");},()->dialog.getButton(-1).setEnabled(true),true);
        }catch(Exception e){url.setError(e.getMessage());}}));dialog.show();
    }
    int pendingCount(JSONObject r){JSONArray rows=r.optJSONArray("webRequests");int count=0;if(rows!=null)for(int i=0;i<rows.length();i++)if("pending".equals(rows.optJSONObject(i).optString("status")))count++;return count;}
    void onlineMenu(JSONObject r) {
        if(!r.has("webId")) {
            String base=OnlineClient.DEFAULT_BASE;
            if(r.has("winner")){toast("Esta rifa já foi encerrada.");return;}
            try {byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);StringBuilder token=new StringBuilder();for(byte b:bytes)token.append(String.format(Locale.US,"%02x",b&255));r.put("webId",UUID.randomUUID().toString()).put("webToken",token.toString()).put("webBase",base);if(!save()){r.remove("webId");r.remove("webToken");r.remove("webBase");return;}}catch(Exception e){toast("Não foi possível criar o link.");return;}
            syncOnline(r,true,()->shareLink(r));return;
        }
        new AlertDialog.Builder(this).setTitle("Pedidos pela internet").setItems(new String[]{"Atualizar e compartilhar link","Pedidos recebidos ("+pendingCount(r)+")","Sincronizar agora"},(d,pos)->{
            if(pos==0)syncOnline(r,true,()->shareLink(r));else if(pos==1){requestsScreen(r);syncOnline(r,true,()->requestsScreen(r));}else syncOnline(r,true,()->toast("Rifa e pedidos atualizados."));
        }).setNegativeButton("Fechar",null).show();
    }
    void shareLink(JSONObject r) {
        String link=r.optString("webBase")+"/?r="+r.optString("webId");
        new AlertDialog.Builder(this).setTitle("Link da rifa").setMessage(link+"\n\nPedidos não confirmam venda ou pagamento. Notificações em segundo plano podem demorar conforme as restrições do Android.").setNegativeButton("Fechar",null).setNeutralButton("Abrir",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link)))).setPositiveButton("Compartilhar",(d,w)->{Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,r.optString("name")+"\nEscolha seus números e envie o pedido:\n"+link);startActivity(Intent.createChooser(share,"Enviar link"));}).show();
    }
    JSONObject publicSnapshot(JSONObject r)throws Exception {
        JSONArray sold=new JSONArray();for(int n=1;n<=r.optInt("total");n++)if(r.optJSONObject("sales").has(""+n))sold.put(n);
        String image="";String path=r.optString("image");if(!path.isEmpty()){Bitmap bitmap=BitmapFactory.decodeFile(path);if(bitmap!=null){int width=bitmap.getWidth(),height=bitmap.getHeight();float scale=Math.min(1f,640f/Math.max(width,height));Bitmap small=Bitmap.createScaledBitmap(bitmap,Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),true);ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,60,out);if(out.size()<180000)image="data:image/jpeg;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);if(small!=bitmap)small.recycle();bitmap.recycle();}}
        return new JSONObject().put("name",r.optString("name")).put("description",r.optString("description")).put("total",r.optInt("total")).put("sold",sold).put("closed",r.has("winner")).put("image",image);
    }
    boolean importRequests(JSONObject r,JSONObject response) {
        String backup=r.toString();JSONObject clearedSales=null,clearedImported=null;
        try {
            JSONArray rows=response.getJSONArray("requests");JSONObject staged=new JSONObject(backup);JSONObject imported=staged.optJSONObject("webImported");if(imported==null)imported=new JSONObject();JSONObject sales=staged.getJSONObject("sales");
            // Cancellation IDs include old deletions outside the normal request-list page.
            Set<String> cancelled=new HashSet<>();JSONArray ids=response.optJSONArray("cancelled");if(ids!=null)for(int i=0;i<ids.length();i++)cancelled.add(ids.optString(i));
            for(int i=0;i<rows.length();i++){JSONObject order=rows.getJSONObject(i);if("rejected".equals(order.optString("status")))cancelled.add(order.optString("id"));}
            Map<String,String> owners=new LinkedHashMap<>();Iterator<String> salesKeys=sales.keys();while(salesKeys.hasNext()){String key=salesKeys.next();owners.put(key,sales.getJSONObject(key).optString("requestId"));}for(String key:RequestNumbers.releasedKeys(owners,cancelled))sales.remove(key);for(String id:cancelled)imported.remove(id);
            clearedSales=new JSONObject(sales.toString());clearedImported=new JSONObject(imported.toString());
            Map<Integer,String> importOwners=new HashMap<>();Iterator<String> availableKeys=sales.keys();while(availableKeys.hasNext()){String n=availableKeys.next();importOwners.put(Integer.parseInt(n),sales.getJSONObject(n).optString("requestId"));}
            Map<String,List<Integer>> acceptedOrders=new LinkedHashMap<>();for(int i=0;i<rows.length();i++){JSONObject order=rows.getJSONObject(i);if("accepted".equals(order.optString("status")))acceptedOrders.put(order.optString("id"),orderNumbers(order));}
            Set<String> importedIds=new HashSet<>();Iterator<String> importedKeys=imported.keys();while(importedKeys.hasNext())importedIds.add(importedKeys.next());
            Set<String> conflicts=RequestImport.conflicts(importOwners,acceptedOrders,importedIds);
            for(int i=0;i<rows.length();i++){JSONObject order=rows.getJSONObject(i);String id=order.optString("id");if(!"accepted".equals(order.optString("status"))||imported.has(id)||conflicts.contains(id))continue;JSONArray nums=order.getJSONArray("numbers");for(int j=0;j<nums.length();j++){String key=""+nums.getInt(j);if(!sales.has(key))sales.put(key,new JSONObject().put("name",order.optString("name")).put("phone",order.optString("phone")).put("paid",true).put("requestId",id));}imported.put(id,true);}
            r.put("webConflicts",new JSONArray(conflicts)).put("sales",sales).put("webImported",imported).put("webRequests",rows).put("webRevision",response.getJSONObject("raffle").getInt("revision"));
            if(!save()){JSONObject old=new JSONObject(backup);r.put("sales",old.getJSONObject("sales"));if(old.has("webImported"))r.put("webImported",old.getJSONObject("webImported"));else r.remove("webImported");return false;}
            try{OrderNotifications.observe(this,profile.optString("id"),new JSONObject(backup),rows);}catch(Exception ignored){}if(!conflicts.isEmpty()){toast("Há pedidos aceitos com números ocupados por vendas locais. Confira Compradores ou exclua o pedido em conflito.");return false;}return true;
        }catch(Exception e){try{if(clearedSales!=null)r.put("sales",clearedSales).put("webImported",clearedImported);r.put("webRequests",response.getJSONArray("requests")).put("webRevision",response.getJSONObject("raffle").getInt("revision"));save();}catch(Exception ignored){}toast(e.getMessage());return false;}
    }
    void waitForAccountSync(JSONObject r,boolean visible,Runnable finished,int attempts){
        if(!foreground||profile==null||!profile.optString("id").equals(r.optString("accountId")))return;
        if(accountBusy){if(attempts>=60){if(visible)toast("A sincronização da conta está demorando. Tente novamente.");return;}onlineHandler.postDelayed(()->waitForAccountSync(r,visible,finished,attempts+1),500);return;}
        boolean live=false;for(int i=0;i<raffles.length();i++)if(raffles.optJSONObject(i)==r)live=true;if(!live){if(visible)toast("A conta foi atualizada. Abra a rifa novamente para conferir os pedidos.");home();return;}
        syncOnline(r,visible,finished);
    }
    void syncOnline(JSONObject r,boolean visible,Runnable finished) {
        if(accountBusy){if(visible){toast("Aguardando a sincronização da conta para atualizar os pedidos.");waitForAccountSync(r,true,finished,0);}return;}
        if(onlineBusy){if(visible)toast("Aguarde a sincronização atual e tente novamente.");return;}if(!r.has("webId"))return;onlineBusy=true;
        final String base=r.optString("webBase"),id=r.optString("webId"),token=r.optString("webToken"),path="/api/raffles/"+id;
        onlineTask(()->{try{return OnlineClient.call(base,path+"/requests","GET",token,-1,null);}catch(Exception e){if(!r.has("webRevision")&&"Rifa não encontrada.".equals(e.getMessage()))return new JSONObject().put("newRaffle",true);throw e;}},response->{
            if(response.optBoolean("newRaffle")){publishSnapshot(r,path,base,token,visible,finished);return;}
            if(!importRequests(r,response)){onlineBusy=false;if(viewingRequests&&current==r)requestsScreen(r);return;}
            publishSnapshot(r,path,base,token,visible,finished);
        },null,visible);
    }
    void publishSnapshot(JSONObject r,String path,String base,String token,boolean visible,Runnable finished) {
        try {JSONObject snapshot=publicSnapshot(r);int revision=r.optInt("webRevision",-1);
            onlineTask(()->OnlineClient.call(base,path,"PUT",token,revision,snapshot),response->{try {r.put("webRevision",response.getInt("revision"));save();}catch(Exception e){toast("Falha ao salvar sincronização.");}onlineBusy=false;if(finished!=null)finished.run();else if(viewingRequests&&current==r){int scroll=((ScrollView)body.getParent()).getScrollY();requestsScreen(r);ScrollView view=(ScrollView)body.getParent();view.post(()->view.scrollTo(0,scroll));}},null,visible);
        }catch(Exception e){onlineBusy=false;if(visible)toast("Não foi possível publicar a rifa.");}
    }
    ArrayList<Integer> orderNumbers(JSONObject order){ArrayList<Integer> nums=new ArrayList<>();JSONArray values=order.optJSONArray("numbers");if(values!=null)for(int i=0;i<values.length();i++)nums.add(values.optInt(i));return nums;}
    boolean activeOrder(JSONObject order){return "pending".equals(order.optString("status"))||"accepted".equals(order.optString("status"));}
    String orderLabel(JSONObject order){
        String date=order.optString("created_at");try{SimpleDateFormat input=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);input.setTimeZone(TimeZone.getTimeZone("UTC"));date=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(input.parse(date));}catch(Exception ignored){}
        String id=order.optString("id");return order.optString("name")+" — "+ReceiptSupport.numbers(orderNumbers(order))+" • "+("accepted".equals(order.optString("status"))?"Aceito":"Pendente")+"\n"+date+" • Ref. "+id.substring(0,Math.min(8,id.length()));
    }
    JSONObject findOrder(JSONObject r,String id){JSONArray rows=r.optJSONArray("webRequests");if(rows!=null)for(int i=0;i<rows.length();i++)if(id.equals(rows.optJSONObject(i).optString("id")))return rows.optJSONObject(i);return null;}
    ArrayList<JSONObject> conflicts(JSONObject r,JSONObject selected){ArrayList<JSONObject> list=new ArrayList<>();Set<Integer> nums=new HashSet<>(orderNumbers(selected));JSONArray rows=r.optJSONArray("webRequests");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject order=rows.optJSONObject(i);if(!activeOrder(order)||order.optString("id").equals(selected.optString("id")))continue;if(RequestNumbers.overlaps(nums,orderNumbers(order)))list.add(order);}return list;}
    void requestsScreen(JSONObject r) {
        current=r;screen("Pedidos recebidos",true);viewingRequests=true;body.addView(label(r.optString("name"),18));
        body.addView(label("Pedidos só viram vendas após você aceitar. Entram como pagos; você pode alterar para não pago.",13));
        if(r.optJSONArray("webConflicts")!=null&&r.optJSONArray("webConflicts").length()>0)body.addView(label("Há pedidos aceitos em conflito com vendas locais. Os outros pedidos continuam disponíveis para aceitar.",14));
        button("Atualizar pedidos",body,()->syncOnline(r,true,()->requestsScreen(r)));
        button("Resolver pedidos duplicados",body,()->managementRefresh(r,()->duplicatePicker(r)));
        button(showAcceptedOrders?"Ocultar pedidos aceitos":"Mostrar pedidos aceitos",body,()->{showAcceptedOrders=!showAcceptedOrders;requestsScreen(r);});
        JSONArray rows=r.optJSONArray("webRequests");if(rows==null||pendingCount(r)==0)body.addView(label("Nenhum pedido pendente. Mantenha internet e notificações habilitadas para receber avisos.",14));
        if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject order=rows.optJSONObject(i);if(!activeOrder(order)||"accepted".equals(order.optString("status"))&&!showAcceptedOrders)continue;
            body.addView(label(orderLabel(order)+"\n"+order.optString("phone"),15));
            if("pending".equals(order.optString("status"))){button("Aceitar pedido (pago)",body,()->resolveRequest(r,order,true));button("Recusar pedido",body,()->resolveRequest(r,order,false));}
            if(!conflicts(r,order).isEmpty())button("Escolher qual pedido manter",body,()->managementRefresh(r,()->duplicatePicker(r)));
            button("Excluir pedido / liberar números",body,()->managementRefresh(r,()->deleteOrderDialog(r,order.optString("id"))));
        }
        button("Voltar à rifa",body,()->detail(r));
    }
    void managementRefresh(JSONObject r,Runnable ready){
        if(onlineBusy||accountBusy){toast("Aguarde a sincronização atual e tente novamente.");return;}if(r.has("winner")){toast("Rifa já sorteada. Seus pedidos estão encerrados.");return;}
        onlineBusy=true;managingOrders=true;
        onlineTask(()->OnlineClient.call(r.optString("webBase"),"/api/raffles/"+r.optString("webId")+"/requests","GET",r.optString("webToken"),-1,null),response->{importRequests(r,response);onlineBusy=false;managingOrders=false;ready.run();},()->managingOrders=false,true);
    }
    void duplicatePicker(JSONObject r){
        ArrayList<JSONObject> options=new ArrayList<>();JSONArray rows=r.optJSONArray("webRequests");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject order=rows.optJSONObject(i);if(activeOrder(order)&&!conflicts(r,order).isEmpty())options.add(order);}
        if(options.isEmpty()){toast("Nenhum pedido com números em comum.");requestsScreen(r);return;}
        String[] names=new String[options.size()];for(int i=0;i<options.size();i++)names[i]=orderLabel(options.get(i));
        new AlertDialog.Builder(this).setTitle("Escolha o pedido que deseja manter").setItems(names,(d,index)->{
            JSONObject keep=options.get(index);ArrayList<JSONObject> discard=conflicts(r,keep);StringBuilder message=new StringBuilder("Manter:\n"+orderLabel(keep)+"\n\nExcluir os pedidos com números em comum:\n");ArrayList<String> ids=new ArrayList<>();for(JSONObject order:discard){ids.add(order.optString("id"));message.append(orderLabel(order)).append("\n");}
            message.append("\nOs números exclusivos dos pedidos excluídos serão liberados. O pedido escolhido ficará aceito. Vendas manuais e outros pedidos serão preservados.");final int revision=r.optInt("webRevision",-1);
            new AlertDialog.Builder(this).setTitle("Confirmar pedido mantido").setMessage(message.toString()).setNegativeButton("Cancelar",null).setPositiveButton("Manter este pedido",(a,b)->manageOrders(r,keep.optString("id"),"keep",ids,revision)).show();
        }).setNegativeButton("Cancelar",null).show();
    }
    void deleteOrderDialog(JSONObject r,String id){
        JSONObject order=findOrder(r,id);if(order==null||!activeOrder(order)){toast("Pedido já removido.");requestsScreen(r);return;}
        final int revision=r.optInt("webRevision",-1);
        new AlertDialog.Builder(this).setTitle("Excluir pedido?").setMessage(orderLabel(order)+"\n\nOs números vendidos vinculados somente a este pedido serão liberados no app e no site. Vendas manuais e outros pedidos serão preservados.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Excluir e liberar",(d,w)->manageOrders(r,id,"delete",new ArrayList<>(Arrays.asList(id)),revision)).show();
    }
    void manageOrders(JSONObject r,String target,String action,ArrayList<String> discard,int revision){
        if(onlineBusy||accountBusy){toast("Aguarde a sincronização atual e tente novamente.");return;}if(r.has("winner")){toast("Rifa já sorteada.");return;}
        try{JSONArray preserve=new JSONArray();JSONObject sales=r.getJSONObject("sales");Iterator<String> keys=sales.keys();while(keys.hasNext()){String key=keys.next();String owner=sales.getJSONObject(key).optString("requestId");if(!discard.contains(owner)&&!(action.equals("keep")&&target.equals(owner)))preserve.put(Integer.parseInt(key));}
            JSONObject data=new JSONObject().put("discard",new JSONArray(discard)).put("preserve",preserve);onlineBusy=true;managingOrders=true;
            String path="/api/raffles/"+r.optString("webId")+"/requests/"+target+"/"+action;
            onlineTask(()->OnlineClient.call(r.optString("webBase"),path,"POST",r.optString("webToken"),revision,data),response->{
                if(action.equals("keep")&&r.optJSONObject("webImported")!=null)r.optJSONObject("webImported").remove(target);
                boolean applied=importRequests(r,response);managingOrders=false;onlineBusy=false;
                if(applied){toast(action.equals("delete")?"Pedido excluído. Números vinculados liberados.":"Pedido mantido. Duplicados removidos.");onlineBusy=true;publishSnapshot(r,"/api/raffles/"+r.optString("webId"),r.optString("webBase"),r.optString("webToken"),true,()->{requestsScreen(r);accountSync(false);});}else requestsScreen(r);
            },()->managingOrders=false,true);
        }catch(Exception e){onlineBusy=false;managingOrders=false;toast("Não foi possível alterar os pedidos. Tente atualizar.");}
    }
    void resolveRequest(JSONObject r,JSONObject order,boolean accept) {
        if(accept&&r.has("winner")){toast("Rifa já sorteada.");return;}
        new AlertDialog.Builder(this).setTitle(accept?"Aceitar pedido?":"Recusar pedido?").setMessage(order.optString("name")+(accept?" terá os números cadastrados como pagos. Você pode alterar para não pago depois.":" terá este pedido recusado.")).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->awaitRequestAction(r,order.optString("id"),accept,0)).show();
    }
    void awaitRequestAction(JSONObject r,String id,boolean accept,int attempts){
        if(!foreground||profile==null||!profile.optString("id").equals(r.optString("accountId")))return;
        if(onlineBusy||accountBusy||managingOrders){if(attempts>=60){toast("A sincronização está demorando. Tente novamente.");return;}onlineHandler.postDelayed(()->awaitRequestAction(r,id,accept,attempts+1),500);return;}
        JSONObject live=null;for(int i=0;i<raffles.length();i++){JSONObject item=raffles.optJSONObject(i);if(r.optString("id").equals(item.optString("id"))){live=item;break;}}
        if(live==null){toast("Rifa não encontrada nesta conta.");return;}performRequestAction(live,id,accept);
    }
    void performRequestAction(JSONObject r,String id,boolean accept){
        if(accept&&r.has("winner")){toast("Rifa já sorteada.");return;}
        try{final JSONObject localSales=new JSONObject(r.getJSONObject("sales").toString());final String base=r.optString("webBase"),token=r.optString("webToken"),path="/api/raffles/"+r.optString("webId");onlineBusy=true;managingOrders=true;
            onlineTask(()->RequestAction.execute(new RequestAction.Steps<JSONObject>(){
                public JSONObject refresh()throws Exception {JSONObject latest=OnlineClient.call(base,path+"/requests","GET",token,-1,null);if(accept&&latest.getJSONObject("raffle").optBoolean("closed"))throw new Exception("Rifa já encerrada.");return latest;}
                JSONObject target(JSONObject latest){JSONArray rows=latest.optJSONArray("requests");if(rows!=null)for(int i=0;i<rows.length();i++)if(id.equals(rows.optJSONObject(i).optString("id")))return rows.optJSONObject(i);return null;}
                public String status(JSONObject latest){JSONObject order=target(latest);return order==null?"missing":order.optString("status");}
                public boolean conflict(JSONObject latest){Set<String> cancelled=new HashSet<>();JSONArray ids=latest.optJSONArray("cancelled");if(ids!=null)for(int i=0;i<ids.length();i++)cancelled.add(ids.optString(i));JSONArray rows=latest.optJSONArray("requests");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject order=rows.optJSONObject(i);if("rejected".equals(order.optString("status")))cancelled.add(order.optString("id"));}for(int n:orderNumbers(target(latest))){JSONObject sale=localSales.optJSONObject(""+n);if(sale!=null&&!cancelled.contains(sale.optString("requestId")))return true;}return false;}
                public JSONObject decide()throws Exception{JSONObject result=OnlineClient.call(base,path+"/requests/"+id+(accept?"/accept":"/reject"),"POST",token,-1,new JSONObject());return result.has("requests")?result:OnlineClient.call(base,path+"/requests","GET",token,-1,null);}
            },accept),response->{
                boolean clean=importRequests(r,response);onlineBusy=false;managingOrders=false;requestsScreen(r);toast(accept?(clean?"Pedido aceito como pago.":"Pedido aceito online. Confira a atualização da lista local."):"Pedido recusado.");accountSync(false);
            },()->managingOrders=false,true);
        }catch(Exception e){onlineBusy=false;managingOrders=false;toast("Não foi possível preparar a decisão do pedido.");}
    }
    @Override protected void onActivityResult(int req,int code,Intent data) {
        super.onActivityResult(req,code,data); if(code!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try {
            if(req==11){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(exportText.getBytes("UTF-8"));} toast("Lista exportada.");}
            if(req==10){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);} if(o.outWidth<1)throw new IOException();o.inSampleSize=1;while(o.outWidth/o.inSampleSize>1600||o.outHeight/o.inSampleSize>1600)o.inSampleSize*=2;o.inJustDecodeBounds=false;Bitmap bitmap;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,o);}if(bitmap==null)throw new IOException();File f=new File(getFilesDir(),"imagem-"+UUID.randomUUID()+".jpg");try(OutputStream out=new FileOutputStream(f)){bitmap.compress(Bitmap.CompressFormat.JPEG,88,out);}pendingImage=f.getAbsolutePath();if(imagePreview!=null)showImage(imagePreview,pendingImage);bitmap.recycle();}
        }catch(Exception e){toast("Não foi possível acessar o arquivo. Tente novamente.");}
    }
    @Override public void onBackPressed() {if(current!=null)home();else super.onBackPressed();}
}
