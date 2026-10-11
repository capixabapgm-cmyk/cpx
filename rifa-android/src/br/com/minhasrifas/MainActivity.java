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
    Runnable onlinePoll=new Runnable(){public void run(){if(!foreground)return;for(int i=0;i<raffles.length();i++){JSONObject r=raffles.optJSONObject((onlineCursor+i)%raffles.length());if(r.has("webId")){onlineCursor=(onlineCursor+i+1)%raffles.length();syncOnline(r,false,null);break;}}onlineHandler.postDelayed(this,30000);}};
    final int green = Color.rgb(24,91,73), ink = Color.rgb(28,43,38);
    int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(green);
        getWindow().setNavigationBarColor(Color.WHITE);
        try { raffles = new JSONArray(getPreferences(0).getString("rifas", "[]")); }
        catch (Exception e) { new AlertDialog.Builder(this).setMessage("Não foi possível ler os dados salvos.").setPositiveButton("OK",null).show(); }
        migratePayments();
        home();
    }
    boolean save() {
        if(getPreferences(0).edit().putString("rifas",raffles.toString()).commit()) {if(foreground&&!onlineBusy){onlineHandler.removeCallbacks(onlinePoll);onlineHandler.postDelayed(onlinePoll,1500);}return true;}
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
        viewingRequests=false;
        LinearLayout root=column(); root.setBackgroundColor(Color.rgb(245,248,246));
        root.setOnApplyWindowInsetsListener((v,insets)-> { v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom()); return insets; });
        TextView header=label((back?"‹  ":"")+title,20); header.setTextColor(Color.WHITE); header.setPadding(dp(12),dp(10),dp(12),dp(10)); header.setBackgroundColor(green);
        if(back) header.setOnClickListener(v->home()); root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); body=column(); body.setPadding(dp(10),dp(8),dp(10),dp(16)); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }
    TextView label(String text,int size) { TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(ink); t.setPadding(0,dp(4),0,dp(4)); return t; }
    Button button(String title, LinearLayout parent, Runnable action) { Button b=new Button(this); b.setText(title); b.setTextSize(14); b.setAllCaps(false); b.setMinHeight(0); b.setMinimumHeight(0); b.setPadding(dp(6),0,dp(6),0); b.setTextColor(Color.WHITE); b.setBackground(bg(green)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(42)); p.setMargins(0,dp(4),0,dp(4)); parent.addView(b,p); b.setOnClickListener(v->action.run()); return b; }
    EditText field(LinearLayout l,String hint,String value,int type) { EditText e=new EditText(this); e.setHint(hint); e.setText(value); e.setInputType(type); l.addView(e,new LinearLayout.LayoutParams(-1,-2)); return e; }
    int sold(JSONObject r) { return r.optJSONObject("sales").length(); }
    String number(int n) { return String.format(Locale.US,"%03d",n); }
    void home() {
        current=null; screen("Minhas Rifas",false);
        body.addView(label("Organize suas rifas, número por número.",16));
        button("+ Adicionar nova rifa",body,()->edit(null));
        button("Configurar pedidos pela internet",body,()->onlineSettings());
        if(raffles.length()==0) body.addView(label("Sua primeira rifa começa aqui. Adicione um nome, uma imagem e a quantidade de números.",18));
        for(int i=0;i<raffles.length();i++) { final JSONObject r=raffles.optJSONObject(i); LinearLayout card=column(); card.setPadding(dp(16),dp(12),dp(16),dp(12)); card.setBackground(bg(Color.WHITE)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(12),0,0); body.addView(card,p); card.addView(label(r.optString("name"),21)); card.addView(label(sold(r)+" de "+r.optInt("total")+" vendidos • "+(r.has("winner")?"Sorteada":"Em andamento"),14)); button("Abrir rifa",card,()->detail(r)); }
    }
    void showImage(ImageView view,String path) {
        if(path==null || path.isEmpty()) { view.setImageResource(br.com.minhasrifas.R.drawable.icon); return; }
        Bitmap b=BitmapFactory.decodeFile(path); if(b!=null)view.setImageBitmap(b); else view.setImageResource(br.com.minhasrifas.R.drawable.icon);
    }
    void edit(JSONObject r) {
        pendingImage=r==null?"":r.optString("image"); LinearLayout form=column(); form.setPadding(dp(18),dp(8),dp(18),dp(8));
        EditText name=field(form,"Nome da rifa",r==null?"":r.optString("name"),1);
        EditText desc=field(form,"Descrição / prêmio",r==null?"":r.optString("description"),android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText total=field(form,"Quantidade de números (1 a 10.000)",r==null?"100":""+r.optInt("total"),2); if(r!=null) total.setEnabled(false);
        imagePreview=new ImageView(this); imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP); form.addView(imagePreview,new LinearLayout.LayoutParams(-1,dp(150))); showImage(imagePreview,pendingImage);
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
        ImageView image=new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP); image.setBackground(bg(Color.rgb(221,235,226))); body.addView(image,new LinearLayout.LayoutParams(-1,dp(110))); showImage(image,r.optString("image"));
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
        if(!r.has("winner") && !ready) button("+ Comprador / vários números",body,()->bulkSale(r));
        body.addView(label("Números • Verde = vendido",14));
        GridLayout grid=new GridLayout(this); body.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        int available=getResources().getDisplayMetrics().widthPixels-dp(20);
        int columns=Math.max(4,Math.min(8,available/dp(46))); grid.setColumnCount(columns);
        int width=available/columns;
        for(int n=1;n<=r.optInt("total");n++) { final int num=n; boolean taken=r.optJSONObject("sales").has(""+n); TextView t=label(number(n),12); t.setPadding(0,0,0,0); t.setGravity(Gravity.CENTER); t.setTextColor(taken?Color.WHITE:green); t.setBackground(bg(taken?green:Color.WHITE)); t.setContentDescription("Número "+n+(taken?", vendido":", disponível")); GridLayout.LayoutParams p=new GridLayout.LayoutParams(); p.width=width-dp(4); p.height=dp(38); p.setMargins(dp(2),dp(2),dp(2),dp(2)); grid.addView(t,p); t.setOnClickListener(v->sale(r,num)); }
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
            TextView status=label(paymentStatus(r,nums)+(r.has("winner")?"":" • tocar para alterar"),12);status.setTextColor(paymentStatus(r,nums).equals("Pago")?green:Color.rgb(166,75,20));row.addView(status);status.setOnClickListener(v->buyerPayment(r,key));
            if(!buyer.optString("phone").isEmpty()) {
                TextView phone=label(buyer.optString("phone"),13); phone.setVisibility(phones.isChecked()?View.VISIBLE:View.GONE);
                phoneRows.add(phone); row.addView(phone);
            }
            row.setOnClickListener(v->receipt(r,key));
            View divider=new View(this); divider.setBackgroundColor(Color.rgb(218,228,222)); body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        body.addView(label("Toque no comprador para ver ou compartilhar o comprovante.",12));
        if(!r.has("winner") && sold(r)<r.optInt("total")) button("+ Comprador / vários números",body,()->bulkSale(r));
        button("Exportar lista (CSV)",body,()->{StringBuilder csv=new StringBuilder("\uFEFFNúmero;Nome;Telefone\r\n"); JSONObject sales=r.optJSONObject("sales");for(int n=1;n<=r.optInt("total");n++){JSONObject b=sales.optJSONObject(""+n);if(b!=null)csv.append(number(n)).append(';').append(csvCell(b.optString("name"))).append(';').append(csvCell(b.optString("phone"))).append("\r\n");} exportText=csv.toString(); Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"compradores.csv");startActivityForResult(i,11);});
        button("Voltar à rifa",body,()->detail(r));
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
        ArrayList<Integer> paidList=new ArrayList<>(), unpaidList=new ArrayList<>();for(int n:nums){if(r.optJSONObject("sales").optJSONObject(""+n).optBoolean("paid",true))paidList.add(n);else unpaidList.add(n);}
        String text=ReceiptSupport.receipt(r.optString("name"),r.optString("description"),buyer.optString("name"),buyer.optString("phone"),nums,new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date()))+"\n\nPagamento: "+paymentStatus(r,nums)+"\nNúmeros pagos: "+(paidList.isEmpty()?"Nenhum":ReceiptSupport.numbers(paidList))+"\nNúmeros não pagos: "+(unpaidList.isEmpty()?"Nenhum":ReceiptSupport.numbers(unpaidList))+"\nSomente os números pagos participam do sorteio.";
        TextView content=label(text,15);content.setPadding(dp(18),dp(8),dp(18),dp(8));content.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(content);
        new AlertDialog.Builder(this).setTitle("Comprovante").setView(scroll).setNegativeButton("Fechar",null).setPositiveButton("Compartilhar",(d,w)->{
            Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_SUBJECT,"Comprovante • "+r.optString("name"));share.putExtra(Intent.EXTRA_TEXT,text);
            try {startActivity(Intent.createChooser(share,"Enviar comprovante pelo WhatsApp ou outro app"));}catch(ActivityNotFoundException e){toast("Nenhum aplicativo disponível para compartilhar.");}
        }).show();
    }
    String csvCell(String s) { if(s.matches("^[=+@-].*"))s="'"+s; return "\""+s.replace("\"","\"\"")+"\""; }
    @Override protected void onResume(){super.onResume();foreground=true;onlineHandler.removeCallbacks(onlinePoll);onlineHandler.postDelayed(onlinePoll,1000);}
    @Override protected void onPause(){super.onPause();foreground=false;onlineHandler.removeCallbacks(onlinePoll);}
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
            String base=getPreferences(0).getString("onlineBase","");if(base.isEmpty()){onlineSettings();return;}
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
        new AlertDialog.Builder(this).setTitle("Link da rifa").setMessage(link+"\n\nPedidos não confirmam venda ou pagamento. O aplicativo recebe pedidos enquanto está aberto e conectado.").setNegativeButton("Fechar",null).setNeutralButton("Abrir",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link)))).setPositiveButton("Compartilhar",(d,w)->{Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,r.optString("name")+"\nEscolha seus números e envie o pedido:\n"+link);startActivity(Intent.createChooser(share,"Enviar link"));}).show();
    }
    JSONObject publicSnapshot(JSONObject r)throws Exception {
        JSONArray sold=new JSONArray();for(int n=1;n<=r.optInt("total");n++)if(r.optJSONObject("sales").has(""+n))sold.put(n);
        String image="";String path=r.optString("image");if(!path.isEmpty()){Bitmap bitmap=BitmapFactory.decodeFile(path);if(bitmap!=null){int width=bitmap.getWidth(),height=bitmap.getHeight();float scale=Math.min(1f,640f/Math.max(width,height));Bitmap small=Bitmap.createScaledBitmap(bitmap,Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),true);ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,60,out);if(out.size()<180000)image="data:image/jpeg;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);if(small!=bitmap)small.recycle();bitmap.recycle();}}
        return new JSONObject().put("name",r.optString("name")).put("description",r.optString("description")).put("total",r.optInt("total")).put("sold",sold).put("closed",r.has("winner")).put("image",image);
    }
    boolean importRequests(JSONObject r,JSONObject response) {
        try {
            JSONArray rows=response.getJSONArray("requests");JSONObject imported=r.optJSONObject("webImported");if(imported==null)imported=new JSONObject();JSONObject sales=r.optJSONObject("sales");
            // Validate every accepted order before importing any numbers.
            for(int i=0;i<rows.length();i++){JSONObject order=rows.getJSONObject(i);if(!"accepted".equals(order.optString("status"))||imported.has(order.optString("id")))continue;JSONArray nums=order.getJSONArray("numbers");for(int j=0;j<nums.length();j++){JSONObject old=sales.optJSONObject(""+nums.getInt(j));if(old!=null&&!order.optString("id").equals(old.optString("requestId")))throw new Exception("Conflito em número já vendido neste celular. Confira os pedidos antes de sincronizar.");}}
            String backup=r.toString();
            for(int i=0;i<rows.length();i++){JSONObject order=rows.getJSONObject(i);String id=order.optString("id");if(!"accepted".equals(order.optString("status"))||imported.has(id))continue;JSONArray nums=order.getJSONArray("numbers");for(int j=0;j<nums.length();j++){String key=""+nums.getInt(j);if(!sales.has(key))sales.put(key,new JSONObject().put("name",order.optString("name")).put("phone",order.optString("phone")).put("paid",false).put("requestId",id));}imported.put(id,true);}
            r.put("webImported",imported).put("webRequests",rows).put("webRevision",response.getJSONObject("raffle").getInt("revision"));
            if(!save()){JSONObject old=new JSONObject(backup);r.put("sales",old.getJSONObject("sales"));if(old.has("webImported"))r.put("webImported",old.getJSONObject("webImported"));else r.remove("webImported");return false;}return true;
        }catch(Exception e){toast(e.getMessage());return false;}
    }
    void syncOnline(JSONObject r,boolean visible,Runnable finished) {
        if(onlineBusy){if(visible)toast("Aguarde a sincronização atual e tente novamente.");return;}if(!r.has("webId"))return;onlineBusy=true;
        final String base=r.optString("webBase"),id=r.optString("webId"),token=r.optString("webToken"),path="/api/raffles/"+id;
        onlineTask(()->{try{return OnlineClient.call(base,path+"/requests","GET",token,-1,null);}catch(Exception e){if(!r.has("webRevision")&&"Rifa não encontrada.".equals(e.getMessage()))return new JSONObject().put("newRaffle",true);throw e;}},response->{
            if(response.optBoolean("newRaffle")){publishSnapshot(r,path,base,token,visible,finished);return;}
            if(!importRequests(r,response)){onlineBusy=false;return;}
            publishSnapshot(r,path,base,token,visible,finished);
        },null,visible);
    }
    void publishSnapshot(JSONObject r,String path,String base,String token,boolean visible,Runnable finished) {
        try {JSONObject snapshot=publicSnapshot(r);int revision=r.optInt("webRevision",-1);
            onlineTask(()->OnlineClient.call(base,path,"PUT",token,revision,snapshot),response->{try {r.put("webRevision",response.getInt("revision"));save();}catch(Exception e){toast("Falha ao salvar sincronização.");}onlineBusy=false;if(finished!=null)finished.run();else if(viewingRequests&&current==r){int scroll=((ScrollView)body.getParent()).getScrollY();requestsScreen(r);ScrollView view=(ScrollView)body.getParent();view.post(()->view.scrollTo(0,scroll));}},null,visible);
        }catch(Exception e){onlineBusy=false;if(visible)toast("Não foi possível publicar a rifa.");}
    }
    void requestsScreen(JSONObject r) {
        current=r;screen("Pedidos recebidos",true);viewingRequests=true;body.addView(label(r.optString("name"),18));
        body.addView(label("Pedidos só viram vendas após você aceitar. Entram como não pagos.",13));
        button("Atualizar pedidos",body,()->syncOnline(r,true,()->requestsScreen(r)));
        JSONArray rows=r.optJSONArray("webRequests");if(rows==null||pendingCount(r)==0)body.addView(label("Nenhum pedido pendente. A sincronização automática funciona com o app aberto e internet.",14));
        if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject order=rows.optJSONObject(i);if(!"pending".equals(order.optString("status")))continue;
            ArrayList<Integer> nums=new ArrayList<>();JSONArray ns=order.optJSONArray("numbers");for(int j=0;j<ns.length();j++)nums.add(ns.optInt(j));
            body.addView(label(order.optString("name")+" — "+ReceiptSupport.numbers(nums)+"\n"+order.optString("phone"),15));
            button("Aceitar pedido (não pago)",body,()->resolveRequest(r,order,true));button("Recusar pedido",body,()->resolveRequest(r,order,false));
        }
        button("Voltar à rifa",body,()->detail(r));
    }
    void resolveRequest(JSONObject r,JSONObject order,boolean accept) {
        if(onlineBusy){toast("Aguarde a sincronização atual.");return;}if(accept&&r.has("winner")){toast("Rifa já sorteada.");return;}
        new AlertDialog.Builder(this).setTitle(accept?"Aceitar pedido?":"Recusar pedido?").setMessage(order.optString("name")+(accept?" terá os números cadastrados como não pagos.":" terá este pedido recusado.")).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->{
            syncOnline(r,true,()->{
                if(accept){JSONArray nums=order.optJSONArray("numbers");for(int i=0;i<nums.length();i++)if(r.optJSONObject("sales").has(""+nums.optInt(i))){toast("Algum número já foi vendido. Combine outros números ou recuse o pedido.");return;}}
                onlineBusy=true;String path="/api/raffles/"+r.optString("webId")+"/requests/"+order.optString("id")+(accept?"/accept":"/reject");
                onlineTask(()->OnlineClient.call(r.optString("webBase"),path,"POST",r.optString("webToken"),-1,new JSONObject()),response->{onlineBusy=false;syncOnline(r,true,()->requestsScreen(r));},null,true);
            });
        }).show();
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
