"""Restyle subscription slides in the existing presentation; drop the optional jcstress slide.
Usage: python restyle_subscription_intro.py input.pptx output.pptx
"""
import sys
from io import BytesIO
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.oxml.xmlchemy import OxmlElement


def text(slide, label, x, y, w, h, size=28, bold=False, font='T-Sans', color='242424'):
    shape=slide.shapes.add_textbox(Inches(x),Inches(y),Inches(w),Inches(h))
    tf=shape.text_frame;tf.word_wrap=True
    tf.margin_top=tf.margin_bottom=tf.margin_left=tf.margin_right=0
    for index,line in enumerate(label.split('\n')):
        p=tf.paragraphs[0] if index==0 else tf.add_paragraph()
        p.text=line;p.font.name=font;p.font.size=Pt(size);p.font.bold=bold
        p.font.color.rgb=RGBColor.from_string(color);p.space_after=Pt(12)
    return shape


def box(slide,label,x,y,w,h,fill='FFFFFF',size=27):
    shape=slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,Inches(x),Inches(y),Inches(w),Inches(h))
    shape.adjustments[0]=.08
    shape.fill.solid();shape.fill.fore_color.rgb=RGBColor.from_string(fill)
    shape.line.color.rgb=RGBColor.from_string('555555');shape.line.width=Pt(1.1)
    tf=shape.text_frame;tf.word_wrap=True;tf.vertical_anchor=3
    tf.margin_top=tf.margin_bottom=Inches(.08)
    for index,line in enumerate(label.split('\n')):
        p=tf.paragraphs[0] if index==0 else tf.add_paragraph()
        p.text=line;p.alignment=2;p.font.name='T-Sans';p.font.size=Pt(size)
        p.font.color.rgb=RGBColor.from_string('242424')
    return shape


def line(slide,x1,y1,x2,y2,arrow=False):
    shape=slide.shapes.add_connector(MSO_CONNECTOR.STRAIGHT,Inches(x1),Inches(y1),Inches(x2),Inches(y2))
    shape.line.width=Pt(1.2);shape.line.color.rgb=RGBColor.from_string('333333')
    if arrow:
        end=OxmlElement('a:tailEnd');end.set('type','triangle');end.set('w','sm');end.set('len','sm')
        shape._element.spPr.get_or_add_ln().append(end)
    return shape


def clean(slide,title,subtitle):
    for shape in list(slide.shapes):slide.shapes._spTree.remove(shape._element)
    text(slide,title,.8,.6,20,1.1,64,True)
    text(slide,subtitle,.8,2.0,20,1.1,29)


def terminal():
    im=Image.new('RGB',(1900,1160),'#1E1E1E');d=ImageDraw.Draw(im)
    d.rectangle((0,0,1900,85),fill='#333333')
    for x,c in [(38,'#FF6058'),(80,'#FFBD2E'),(122,'#28C840')]:d.ellipse((x,28,x+23,51),fill=c)
    f=ImageFont.truetype('/System/Library/Fonts/Monaco.ttf',36)
    small=ImageFont.truetype('/System/Library/Fonts/Monaco.ttf',29)
    d.text((230,23),'Market Pulse — subscriptions --workers 3',font=small,fill='#CCCCCC')
    rows=[('$ watch SBER 3s 30s','#A8D08D'),('Подписка принята: SBER','#DDDDDD'),
          ('$ watch GAZP 5s 1m','#A8D08D'),('Подписка принята: GAZP','#DDDDDD'),
          ('[SBER] 250.00','#F3F3F3'),('[GAZP] 120.50','#F3F3F3'),
          ('$ remove SBER','#FFD966'),('Подписка удалена: SBER','#DDDDDD'),
          ('[GAZP] 120.50','#F3F3F3'),('$ exit','#A8D08D'),('Подписки и пул остановлены','#DDDDDD')]
    for i,(label,color) in enumerate(rows):d.text((40,120+i*86),label,font=f,fill=color)
    stream=BytesIO();im.save(stream,format='PNG');stream.seek(0);return stream


def restyle(presentation):
    # Target by title so adding/removing other slides does not break the script.
    def find(title):
        return next(s for s in presentation.slides if any(sh.has_text_frame and sh.text in ({title, "Когда запускать обновление?"} if title=="Когда выполнять следующее обновление?" else {title}) for sh in s.shapes))
    s=find('Подписки на котировки')
    clean(s,'Подписки на котировки','Указываем тикер, интервал обновления и срок подписки')
    s.shapes.add_picture(terminal(),Inches(.9),Inches(3.4),width=Inches(12.0))
    text(s,'Ввод доступен,\nпока запросы ждут сеть.',14,3.6,6.4,1.6,32)
    text(s,'Не более N загрузок\nодновременно.',14,6.0,6.4,1.6,32)
    text(s,'remove — одна подписка\nexit — весь сервис',14,8.4,6.4,1.7,31)

    s=find('Как устроим решение')
    clean(s,'Как устроим решение','Платформенные потоки: ввод, диспетчер и N работников')
    box(s,'Основной поток\nstdin',1.0,4.4,3.4,1.4,'DDEBF7')
    box(s,'Диспетчер\nсроки и интервалы',6.0,4.4,3.8,1.4,'E2EFD9')
    line(s,4.4,5.1,6.0,5.1,True);text(s,'watch / remove',4.35,3.6,2.2,.5,19)
    box(s,'Стек задач',11.2,3.65,3.1,3.7,'FFFFFF',25)
    # The stack label belongs above its task cells, not centred behind them.
    s.shapes[-1].text_frame.clear()
    text(s,'Treiber · LIFO',11.35,3.82,2.85,.5,24)
    for i,label in enumerate(['последняя задача','задача','первая задача']):
        box(s,label,11.45,4.6+i*.72,2.6,.6,'FFF2CC',20)
    line(s,9.8,5.1,11.2,5.1,True);text(s,'submit',10,4.4,1.2,.5,20)
    for y,label in [(3.6,'Worker 1'),(5.5,'Worker 2'),(7.4,'Worker N')]:
        box(s,label+'\nHTTP-запрос',16, y,4.0,1.35,'DDEBF7',25)
    line(s,14.3,5.1,15.2,5.1)
    line(s,15.2,4.275,15.2,8.075)
    for y in [4.275,6.175,8.075]:line(s,15.2,y,16,y,True)
    text(s,'pop',14.35,3.5,1.0,.5,20)
    line(s,18,8.75,18,9.4);line(s,18,9.4,7.9,9.4);line(s,7.9,9.4,7.9,5.8,True)
    text(s,'завершение → следующий срок обновления',9.0,9.6,10,.7,25)
    text(s,'Нет задач → worker паркуется. Появилась задача → unpark.',1.0,10.8,19.8,.7,28)

    s=find('Когда выполнять следующее обновление?')
    clean(s,'Когда запускать обновление?','Интервал считаем после ответа: одна загрузка на подписку, без перекрытия')
    text(s,'Диспетчер',.9,3.8,2.7,.6,27)
    text(s,'Работник',.9,6.3,2.7,.6,27)
    line(s,3.5,4.3,20.1,4.3,True);line(s,3.5,6.9,20.1,6.9,True)
    box(s,'запрос 1',4.2,6.35,3.3,1.1,'DDEBF7',27)
    box(s,'запрос 2',12.5,6.35,3.3,1.1,'DDEBF7',27)
    for x in [4.2,12.5]:
        line(s,x,4.3,x,6.35,True)
        text(s,'submit',x+.15,4.65,1.7,.5,23)
    line(s,7.5,5.8,12.5,5.8)
    line(s,7.5,5.55,7.5,6.05);line(s,12.5,5.55,12.5,6.05)
    text(s,'интервал 3 с',8.4,4.9,3.5,.6,27)
    text(s,'работник свободен',8.0,7.8,4.5,.6,25)
    text(s,'Срок истёк → новые загрузки не начинаем; уже начатую разрешаем закончить.',1.0,9.15,19.5,.9,29)
    text(s,'remove TICKER → отмена подписки и её запроса.  exit → остановка всех потоков.',1.0,10.4,19.5,.9,28)
    for slide in list(presentation.slides):
        if any(sh.has_text_frame and sh.text=='Тестируем многопоточность' for sh in slide.shapes):
            element=next(el for el in presentation.slides._sldIdLst if el.rId==next(
                rel.rId for rel in presentation.part.rels.values() if rel.target_part==slide.part))
            presentation.part.drop_rel(element.rId)
            presentation.slides._sldIdLst.remove(element)
    # Refresh both inherited number placeholders and the explicitly added number boxes.
    for number,slide in enumerate(presentation.slides,1):
        for shape in slide.shapes:
            if shape.has_text_frame and shape.text.strip().isdigit():
                for paragraph in shape.text_frame.paragraphs:
                    if paragraph.runs:
                        paragraph.runs[0].text=str(number)
                        for run in paragraph.runs[1:]:run.text=''
        if slide in [presentation.slides[4],presentation.slides[5],presentation.slides[6]]:
            text(slide,str(number),20.3,11.6,.5,.3,12)


if __name__=='__main__':
    p=Presentation(sys.argv[1]);restyle(p);p.save(sys.argv[2]);print(len(p.slides))
