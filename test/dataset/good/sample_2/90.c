/****************************************************
 *        F17.CPP                                   *
 *  Copyright (C) Михаил М. Баранов                 *
 *  Модификация без предварительного согласия автора*
 *  будет рассматривается как нарушение авторского  *
 *  и имущественного права                          *
 *  создан: 21 Сентября 1995, Четверг               *
 ****************************************************/
#include "common.h"
#include "doc.h"

void ChooseTheDocs(_FileView *fv);

void DocReport(char *name);

static short ShowMask17[]={
       1,   // Номер_договора
       //2,   // Тип_договора
       3,   // Дата_заключения
       //4,   // Код_партнера
       //5,   // Дата_начала
       6,   // Дата_окончания
       //7,   // Код_получателя
       //8,   // Содержание
       //9,   // Состояние_договора
       //10,   // Примечание
       11,  //Краткое_описание
       //12 Признак закрытия
       //13 Признак счета
       //14 Район
       //15 Тип финансирования
       0    // end_of_list
};
static file_17 prev;
file_17 * rf17;
Table *T17;
static char  * f17;
short DocType=0;
static short Filter = 0;
static file_17_2 fff;
_Static *fltr;

void Init17(void){
  T17= new Table("Договор",OWNER);
  if(!T17->opened){
	printf("Error: file Договор not opened\n");
        exit(0);
  }
  f17=new char [T17->max_rec_len];
  rf17 = (file_17*)f17;
};

void Close17(void){
  delete T17;
  delete f17;
};

static void WriteF17(void *rec, char *buf){
    int curlen=0,sz;
    buf[0]=0;
    char *buf2;
    int i=0;
    while(ShowMask17[i]){
      sz=T17->SizeForPrint(ShowMask17[i]);
      if(curlen+sz+1<MAX_STRING_WIDTH){
        buf2 = new char[sz+1];
        memset(buf2,0,sz+1);
        T17->PrintField(ShowMask17[i], rec ,buf2);
        strcat(buf,buf2);
        strcat(buf,"│");;
        curlen=curlen+sz+1;
        delete buf2;
      }
      i++;
    }
};


static void EditF17Done(_Event *E,void *parent){
  int OK=1;
  ((_Window*) parent)->LostFocus(1);
  if(rf17->Data_zakluheniy.Empty())  OK=0;
  if(rf17->Data_nahala.Empty()) OK=0;
  if(rf17->Data_okonhaniy.Empty()) OK=0;
  if(OK && rf17->Data_okonhaniy < rf17->Data_nahala)
    OK=0;
  if(!OK){
    MessageBox(
      "Проверьте правильность установленных дат:",
      "1. Даты не следует оставлять пустыми",
      "2. Дата окончания не должна быть больше",
      "   чем дата начала договора"
      );
     E->ClearEvent((_Window*)parent);
    ((_Window*) parent)->GotFocus(1);
     return;
  }

  if(rf17->Sostoynie_dogovora>1){
    if(MessageBox("Перевести договор в разряд завершенных ?",
               "       Да - [Enter] Нет - [Esc]"))
      rf17->Priznak_zakritiy=1;
    else
     rf17->Sostoynie_dogovora=1;
  }

  _FileView *fv= (_FileView*)(((_Window *) parent)->parent);

  // проверяем можно ли менять номер
  if(memcmp(prev.Nomer_dogovora,rf17->Nomer_dogovora,13)){
    if(!T22->GEQ(rf22,prev.Nomer_dogovora,0) ||
       !T18->GEQ(rf18,prev.Nomer_dogovora,0) ||
       !T27->GEQ(rf27,prev.Nomer_dogovora,0)
       ){
         MessageBox( "         Нельзя менять номер договора!",
                     "Есть строки статей (F2) или строки оплат (F3)",
                     "или карточки договора (F4)",
                     "          нажмите Esc для продолжения"
                     );
         memcpy(rf17->Nomer_dogovora,prev.Nomer_dogovora,13);
       }
  }
  if(!rf17->Priznak_zakritiy)
    fv->Update(f17);
  else{
    long ptr=fv->CurRecPos();
    T17->GDir(&prev,&ptr,3);
    T17->Update(rf17);
    fv->Repaint();
  }
  E->msg.K=ESC;
};

static void InsertF17Done(_Event *E,void *parent){
  int OK=1;
  ((_Window*) parent)->LostFocus(0);
  if(rf17->Data_zakluheniy.Empty())  OK=0;
  if(rf17->Data_nahala.Empty()) OK=0;
  if(rf17->Data_okonhaniy.Empty()) OK=0;
  if(OK && rf17->Data_okonhaniy < rf17->Data_nahala)
    OK=0;
  if(!OK){
    MessageBox(
      "Проверьте правильность установленных дат:",
      "1. Даты не следует оставлять пустыми",
      "2. Дата окончания не может быть больше",
      "   чем дата начала договора"
      );
  }

  _FileView *fv= (_FileView*)(((_Window *) parent)->parent);
  fv->Insert(f17);
  E->msg.K=ESC;
};

static void EditF17(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  if(!fv->Lock(f17)) return;
  memcpy(&prev,rf17,sizeof(file_17));
  _Window *w= new _Window(1,1,74,1,E_STYLE17,stBuffered | stFrame,"InsertF17");
  fv->Add(w); short wh=1;   w->HelpID=19;
  w->Add(new _Static(1,(wh),E_STYLE17,"Номер_договора      "));
  w->Add(BuildCell(T17,1,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Тип_договора        "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(2)),0,15,0,2 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Дата_заключения     "));
  w->Add(BuildCell(T17,3,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Дата_начала         "));
  w->Add(BuildCell(T17,5,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Дата_окончания      "));
  w->Add(BuildCell(T17,6,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Состояние_договора  "));
  w->Add(new _ClsrCell (21,(wh++),30,E_STYLE17,(short*)(f17+T17->FieldOffset(9)),6));

  w->Add(new _Static(1,(wh),E_STYLE17,"Код_партнера        "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(4)),0,20,0,2 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Код_получателя      "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(7)),0,20,0,2 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Район               "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(14)),0,14,0,2 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Код финансирования  "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(15)),0,26,0,2 ));

  w->Add(new _Static(1,(wh++),E_STYLE17,"Краткое_описание    "));
  w->Add(BuildCell(T17,11,f17,1,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh++),E_STYLE17,"Содержание          "));
  w->Add(BuildCell(T17,8,f17,1,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh++),E_STYLE17,"Примечание          "));
  w->Add(BuildCell(T17,10,f17,1,&wh,w->w,E_STYLE17 ));


  //wh++;
  if(!DocType)
    w->AddCallBack(EditF17Done,evKey,ENTER);
  w->GotFocus();
  E->ClearEvent((_Window*)parent);
};

static void InsertF17(_Event *E, void * parent){
  if(DocType==1){
   E->ClearEvent((_Window*)parent);
   return;
  }
  _FileView *fv= (_FileView*) parent;
  memset(f17,0,sizeof(f17));
  InitRecord(T17,f17);
  _Window *w= new _Window(1,1,74,1,E_STYLE17,stBuffered | stFrame,"InsertF17");
  fv->Add(w); short wh=1;   w->HelpID=19;
  w->Add(new _Static(1,(wh),E_STYLE17,"Номер_договора      "));
  w->Add(BuildCell(T17,1,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Тип_договора        "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(2)),0,15,0,2 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Дата_заключения     "));
  w->Add(BuildCell(T17,3,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Дата_начала         "));
  w->Add(BuildCell(T17,5,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Дата_окончания      "));
  w->Add(BuildCell(T17,6,f17,21,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Состояние_договора  "));
  w->Add(new _ClsrCell (21,(wh++),30,E_STYLE17,(short*)(f17+T17->FieldOffset(9)),6));

  w->Add(new _Static(1,(wh),E_STYLE17,"Код_партнера        "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(4)),0,20,0,2 ));


  w->Add(new _Static(1,(wh),E_STYLE17,"Код_получателя      "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(7)),0,20,0,2 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Район               "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(14)),0,14,0,2 ));

  w->Add(new _Static(1,(wh),E_STYLE17,"Код финансирования  "));
  w->Add(new _RelSCell(21,(wh++),50,E_STYLE17,5,(short*)(f17+T17->FieldOffset(15)),0,26,0,2 ));

  w->Add(new _Static(1,(wh++),E_STYLE17,"Краткое_описание    "));
  w->Add(BuildCell(T17,11,f17,1,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh++),E_STYLE17,"Содержание          "));
  w->Add(BuildCell(T17,8,f17,1,&wh,w->w,E_STYLE17 ));

  w->Add(new _Static(1,(wh++),E_STYLE17,"Примечание          "));
  w->Add(BuildCell(T17,10,f17,1,&wh,w->w,E_STYLE17 ));

  //wh++;
  w->AddCallBack(InsertF17Done,evKey,ENTER);
  w->GotFocus();
  E->ClearEvent((_Window*)parent);
};

static void DeleteF17(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  if(!fv->Lock(f17)) return;
  if( !T22->GEQ(rf22,rf17->Nomer_dogovora,0) ||
      !T18->GEQ(rf18,rf17->Nomer_dogovora,0) ||
      !T27->GEQ(rf27,rf17->Nomer_dogovora,0)
    ){
        MessageBox( "Нельзя удалить строку договора",
                    "Есть строки статей (F2)",
                    "или строки оплат (F3)",
                    "или карточки договора (F4)",
                    "нажмите Esc для продолжения"
                    );
     }else
        fv->Delete();
  E->ClearEvent((_Window*)parent);
};

static void PrintF17(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  fv->Lock(rf17);
  char name[16];
  DocReport(name);
  _ShowPrint *sp = new _ShowPrint(name,"Печать Договор");
  fv->Add(sp); sp->HelpID=100;
  sp->GotFocus();
  E->ClearEvent((_Window*)parent);
};

static void CB18(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  if(!fv->Lock(rf17)) return;
  AddF18(0,fv);
  E->ClearEvent((_Window*)parent);
};
static void CB22(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  if(!fv->Lock(rf17)) return;
  AddF22(0,fv);
  E->ClearEvent((_Window*)parent);
};

static void CB27(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  if(!fv->Lock(rf17)) return;
  AddF27(0,fv);
  E->ClearEvent((_Window*)parent);
};
static void FlipFilter(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  Filter=1-Filter;
  fff.Priznak_zakritiy=DocType;
  if(fv->Lock(rf17))
    fff.Tip_dogovora=rf17->Tip_dogovora;
  else
    fff.Tip_dogovora=0;

  if(Filter){
    {
      _Window *w= new _Window(0,0,53,1,E_STYLE17,stBuffered | stFrame,"FilterF17");
      short wh=1;
      w->CenterScr();
      w->Add(new _Static(1,(wh++),E_STYLE17,"Опpеделите тип договоpа для задания фильтpа"));
      w->Add(new _RelSCell(1,(wh),50,E_STYLE17,5,&(fff.Tip_dogovora),0,15,0,2 ));
      w->Exec();
      delete w;
    }
    fv->Del(fltr);
    fv->Add(fltr=new _Static(12,0,V_STYLE17,"/фильтр/"));
    fv->ChangeFilter(2, &fff, 4);
  }else{
    fv->Del(fltr);
    fv->Add(fltr=new _Static(12,0,V_STYLE17,"/  все /"));
    fv->ChangeFilter(2, &fff, 2);
  }
  E->ClearEvent((_Window*)parent);
};
static void Choose(_Event *E, void * parent){
  _FileView *fv= (_FileView*) parent;
  ChooseTheDocs(fv);
  E->ClearEvent((_Window*)parent);
};


void AddF17( void (*CallBackFunc)(_Event *,void *), _Window * parent){
  if(!Dostup[17])
    return;
  _Window * tmp =parent;
  if(!tmp) tmp =GDt;
  if(tmp->Find("Договор")){
      tmp->Del(tmp->Find("Договор"));
  }
  fff.Priznak_zakritiy=DocType;
  Filter=0;
  _FileView * fv=new _FileView (T17,2,&fff,2,WriteF17,2,2,75,20,1,V_STYLE17,"Договор");
  fv->HelpID=8;
  fv->Add(new _Static(2,0,V_STYLE17," Договор "));
  fv->Add(fltr=new _Static(12,0,V_STYLE17,"/  все /"));
  if(Dostup[17]==D_WRITE){
    fv->AddCallBack(EditF17, evKey,ENTER);
    if(!DocType){
      fv->AddCallBack(InsertF17, evKey,INSERT);
      fv->AddCallBack(DeleteF17, evKey,DELETE);
    }
  }
  fv->AddCallBack(PrintF17, evKey, CTRL_P);
  fv->AddCallBack(CB18, evKey,F3);
  fv->AddCallBack(CB22, evKey,F2);
  fv->AddCallBack(CB27, evKey,F4);
  fv->AddCallBack(FlipFilter, evKey,F6);
  fv->AddCallBack(Choose, evKey,F7);
  if(CallBackFunc)
        fv->AddCallBack(CallBackFunc, evKey, ESC);
  tmp->Add(fv);
  tmp->SwitchFocus(fv);
};


