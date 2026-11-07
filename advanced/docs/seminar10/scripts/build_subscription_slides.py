"""Добавляет сквозную задачу к текущему PPTX, сохраняя исходные слайды и layouts.
Usage: python build_subscription_slides.py input.pptx output.pptx
Requires python-pptx and Pillow. Новые диаграммы — редактируемые фигуры PowerPoint.
"""
import sys
from io import BytesIO
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR

source, destination = map(Path, sys.argv[1:3])
p = Presentation(source)
original = list(p.slides)
if any('Подписки на котировки' in sh.text for s in original for sh in s.shapes if sh.has_text_frame):
    raise SystemExit('Блок подписок уже добавлен: используйте исходный PPTX')
W = p.slide_width / 914400
H = p.slide_height / 914400
C = {'ink':'242424', 'gray':'F1F1F1', 'yellow':'FFDD2D', 'green':'DCEFD5', 'blue':'DEEBF7', 'white':'FFFFFF'}
content_layout = original[30].slide_layout
section_layout = original[3].slide_layout

def text(s, label, x, y, w, h, size=28, bold=False, color='242424', font='T-Sans'):
    sh=s.shapes.add_textbox(Inches(x),Inches(y),Inches(w),Inches(h))
    tf=sh.text_frame;tf.word_wrap=True
    tf.margin_left=tf.margin_right=0
    tf.margin_top=tf.margin_bottom=0
    for i,line in enumerate(label.split('\n')):
        pa=tf.paragraphs[0] if i==0 else tf.add_paragraph()
        pa.text=line;pa.font.name=font;pa.font.size=Pt(size);pa.font.bold=bold;pa.font.color.rgb=RGBColor.from_string(color)
        pa.space_after=Pt(10)
    return sh

def rect(s,label,x,y,w,h,fill='gray',size=28):
    sh=s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,Inches(x),Inches(y),Inches(w),Inches(h))
    sh.fill.solid();sh.fill.fore_color.rgb=RGBColor.from_string(C.get(fill,fill));sh.line.color.rgb=RGBColor.from_string('888888');sh.line.width=Pt(1)
    tf=sh.text_frame;tf.word_wrap=True;tf.margin_left=Inches(.2);tf.margin_right=Inches(.2)
    tf.vertical_anchor=1
    for i,line in enumerate(label.split('\n')):
        pa=tf.paragraphs[0] if i==0 else tf.add_paragraph();pa.text=line;pa.alignment=2
        pa.font.name='T-Sans';pa.font.size=Pt(size);pa.font.color.rgb=RGBColor.from_string(C['ink'])
    return sh

def arrow(s,x,y,w,h):
    sh=s.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, Inches(x),Inches(y),Inches(w),Inches(h))
    sh.fill.solid();sh.fill.fore_color.rgb=RGBColor.from_string('555555');sh.line.fill.background()

def slide(title, subtitle=None, section=False):
    s=p.slides.add_slide(section_layout if section else content_layout)
    for sh in list(s.shapes):
        s.shapes._spTree.remove(sh._element)
    text(s,title,1.0,1.0,W-2,1.2,64,bold=True,color='FFFFFF' if section else C['ink'])
    if subtitle:text(s,subtitle,1.0,2.35,W-2,1.2,30,color='FFFFFF' if section else C['ink'])
    return s

intro=[];final=[]
s=slide('Котировки по расписанию','Сквозная задача: от платформенных потоков к собственному исполнителю',True);intro.append(s)
s=slide('Подписки на котировки','Пользователь задаёт инструмент, интервал обновления и срок подписки');intro.append(s)
rect(s,'watch SBER 3s 30s\nwatch GAZP 5s 1m\n\n[SBER] 250.40\n[GAZP] 142.10\n\nexit',1.0,4.0,9.2,5.8,size=34)
text(s,'Ввод остаётся доступным\nЗапросы идут в фоне\nОдновременно работают не более N загрузок',11.2,4.1,9.0,4.0,34)
rect(s,'Первый запрос — сразу. Ошибка одного обновления не останавливает подписки.',11.2,8.3,9.0,1.5,fill='yellow',size=25)
s=slide('Как устроим решение','Ожидание времени и ожидание сети поручаем разным потокам');intro.append(s)
for label,x,w,fill in [('Ввод команд\nосновной поток',1.0,4.0,'blue'),('Диспетчер\nсроки и интервалы',6.1,4.0,'green'),('Стек задач\nТрайбер · LIFO',11.2,4.0,'yellow'),('N работников\nHTTP-запросы',16.3,4.0,'blue')]:rect(s,label,x,4.5,w,2.0,fill=fill,size=27)
for x in [5.15,10.25,15.35]:arrow(s,x,5.2,.8,.5)
text(s,'submit → CAS-публикация задачи → unpark → извлечение и выполнение',1.0,7.6,19.5,1.0,32)
text(s,'Работник без задач паркуется. Между обновлениями подписки работник свободен.',1.0,9.0,19.5,1.0,28)
s=slide('Когда выполнять следующее обновление?','Для одной подписки — не более одной задачи в очереди или в работе');intro.append(s)
rect(s,'Запрос',1.0,4.8,4.0,1.5,fill='blue',size=32)
rect(s,'Интервал 3 с\nработник свободен',6.3,4.8,5.0,1.5,fill='gray',size=28)
rect(s,'Следующий запрос',12.6,4.8,6.0,1.5,fill='blue',size=32)
arrow(s,5.25,5.3,.8,.5);arrow(s,11.55,5.3,.8,.5)
text(s,'Интервал отсчитываем от завершения запроса — fixed delay.',1.0,7.3,19.0,1.0,32)
text(s,'Срок подписки истёк → новые запросы не начинаем.\nУже начатый запрос может закончиться.\nexit → отбрасываем ожидающие задачи, прерываем работников, ждём завершения.',1.0,8.5,19.0,2.4,28)
s=slide('Стек Трайбера: публикуем задачу через CAS','Новый узел готовим локально. Меняем head только если вершина не изменилась.');final.append(s)
code='''Node observed;
Node next;
do {
    observed = head.get();
    next = new Node(task, observed);
} while (!head.compareAndSet(observed, next));'''
fontpath='/System/Library/Fonts/Monaco.ttf'
if not Path(fontpath).exists():fontpath='/System/Library/Fonts/Menlo.ttc'
f=ImageFont.truetype(fontpath,34)
im=Image.new('RGB',(1150,460),'#F1F1F1');d=ImageDraw.Draw(im)
for i,line in enumerate(code.splitlines()):d.text((25,20+i*58),line,font=f,fill='#243C5A')
b=BytesIO();im.save(b,format='PNG');b.seek(0);s.shapes.add_picture(b,Inches(1),Inches(4.5),width=Inches(11.8))
rect(s,'head → новый узел',14.0,4.7,6.1,1.4,fill='yellow',size=28)
rect(s,'task  |  next → прежняя вершина',14.0,6.8,6.1,1.4,fill='blue',size=25)
text(s,'AtomicReference уже даёт гарантии памяти.\nОтдельный volatile для head не нужен.',1.0,9.8,19.5,1.4,30)
s=slide('Пробуждение и остановка','Permit закрывает промежуток между проверкой пустоты и park');final.append(s)
rect(s,'Работник\npop → пусто',1.0,4.2,5.4,1.8,fill='blue',size=30)
rect(s,'Отправитель\npush → unpark',8.0,4.2,5.4,1.8,fill='yellow',size=30)
rect(s,'Работник\npark → цикл снова',15.0,4.2,5.4,1.8,fill='blue',size=30)
arrow(s,6.7,4.9,1.0,.5);arrow(s,13.7,4.9,1.0,.5)
text(s,'shutdown: запретить submit → очистить стек → interrupt / unpark → join',1.0,7.5,19.5,1.4,31)
text(s,'Учебные ограничения: LIFO без справедливости, неограниченный стек, пробуждение всех работников.\nНа следующем семинаре сравним с ExecutorService и ScheduledExecutorService.',1.0,9.2,19.5,1.6,27)
s=slide('Проверим подписки в работе','Несколько инструментов, медленный ответ и exit во время загрузки',True);final.append(s)
# Insert the introduction after the existing seminar plan, and the implementation after the last HB slide.
ids=list(p.slides._sldIdLst)
intro_ids=ids[len(original):len(original)+len(intro)]
final_ids=ids[len(original)+len(intro):]
hb=max(i for i,s in enumerate(original) if any(sh.has_text_frame and sh.text.strip()=='Happens-Before' for sh in s.shapes))
first_experiment=next(i for i,s in enumerate(original) if any(sh.has_text_frame and sh.text.startswith('Каков результат выполнения') for sh in s.shapes))
order=ids[:3]+intro_ids+ids[3:hb+1]+final_ids+ids[hb+1:len(original)]
for el in list(p.slides._sldIdLst):p.slides._sldIdLst.remove(el)
for el in order:p.slides._sldIdLst.append(el)
# Correct slide number placeholders, keeping all other original text untouched.
for number,s in enumerate(p.slides,1):
    for sh in s.shapes:
        if sh.is_placeholder and sh.has_text_frame and sh.text.strip().isdigit():
            for para in sh.text_frame.paragraphs:
                if para.runs:
                    para.runs[0].text=str(number)
                    for run in para.runs[1:]:run.text=''
    if s in intro or s in final:
        text(s,str(number),W-1.3,H-.6,.6,.3,12)
# Presenter notes tie existing theory to the ongoing code, without demo links on slides.
notes={
 'Программное управление состоянием потока':'Сквозной пример: TreiberThreadPool.work, park/unpark. Пока читаем контракт, CAS подробно после JMM.',
 'Создание потоков (современный вариант – Java 21+)':'Сквозной пример: создание N workers и отдельного dispatcher; каждый поток имеет одну роль.',
 'Обработка исключений в потоке':'Сквозной пример: RuntimeException запроса изолируется; необработанный Error работника — отдельный отказ, не обещаем автоматический перезапуск.',
 'Прерывание работы потока':'Сквозной пример: close → shutdown → interrupt. Сравнить HttpClient и URLConnection в отдельном демо.',
 'Методы wait & notify из класса Object':'Сквозной пример: gate защищает подписки; dispatcher ждёт в цикле, HTTP выполняется вне монитора.',
 'Ключевое слово volatile':'Сквозной пример: stopped и AtomicReference head. Plain-флаг не публикует данные; использовать PublicationPlain/Volatile в jcstress.'}
for s in original:
    texts=[sh.text.strip() for sh in s.shapes if sh.has_text_frame]
    for title,note in notes.items():
        if title in texts:
            tf=s.notes_slide.notes_text_frame
            if tf:tf.text=(tf.text+'\n'+note).strip()
from restyle_subscription_intro import restyle
restyle(p)
p.save(destination)
print(f'{len(original)} → {len(p.slides)} slides: {destination}')
