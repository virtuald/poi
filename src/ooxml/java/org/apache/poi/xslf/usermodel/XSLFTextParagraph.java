/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package org.apache.poi.xslf.usermodel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.usermodel.AutoNumberingScheme;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PaintStyle.SolidPaint;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.model.ParagraphPropertyFetcher;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextAutonumberBullet;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBulletSizePercent;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBulletSizePoint;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharBullet;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextField;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextFont;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextLineBreak;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextNormalAutofit;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextSpacing;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextTabStop;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextTabStopList;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextAlignType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextAutonumberScheme;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextFontAlignType;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPlaceholder;
import org.openxmlformats.schemas.presentationml.x2006.main.STPlaceholderType;

/**
 * Represents a paragraph of text within the containing text body.
 * The paragraph is the highest level text separation mechanism.
 *
 * @author Yegor Kozlov
 * @since POI-3.8
 */
@Beta
public class XSLFTextParagraph implements TextParagraph<XSLFShape,XSLFTextParagraph,XSLFTextRun> {
    private final CTTextParagraph _p;
    private final List<XSLFTextRun> _runs;
    private final XSLFTextShape _shape;

    XSLFTextParagraph(CTTextParagraph p, XSLFTextShape shape){
        _p = p;
        _runs = new ArrayList<XSLFTextRun>();
        _shape = shape;

        for(XmlObject ch : _p.selectPath("*")){
            if(ch instanceof CTRegularTextRun){
                CTRegularTextRun r = (CTRegularTextRun)ch;
                _runs.add(new XSLFTextRun(r, this));
            } else if (ch instanceof CTTextLineBreak){
                CTTextLineBreak br = (CTTextLineBreak)ch;
                CTRegularTextRun r = CTRegularTextRun.Factory.newInstance();
                r.setRPr(br.getRPr());
                r.setT("\n");
                _runs.add(new XSLFTextRun(r, this));
            } else if (ch instanceof CTTextField){
                CTTextField f = (CTTextField)ch;
                CTRegularTextRun r = CTRegularTextRun.Factory.newInstance();
                r.setRPr(f.getRPr());
                r.setT(f.getT());
                _runs.add(new XSLFTextRun(r, this));
            }
        }
    }

    public String getText(){
        StringBuilder out = new StringBuilder();
        for (XSLFTextRun r : _runs) {
            out.append(r.getRawText());
        }
        return out.toString();
    }

    String getRenderableText(){
        StringBuilder out = new StringBuilder();
        for (XSLFTextRun r : _runs) {
            out.append(r.getRenderableText());
        }
        return out.toString();
    }

    @Internal
    public CTTextParagraph getXmlObject(){
        return _p;
    }

    public XSLFTextShape getParentShape() {
        return _shape;

    }

    @Override
    public List<XSLFTextRun> getTextRuns(){
        return _runs;
    }

    public Iterator<XSLFTextRun> iterator(){
        return _runs.iterator();
    }

    /**
     * Add a new run of text
     *
     * @return a new run of text
     */
    public XSLFTextRun addNewTextRun(){
        CTRegularTextRun r = _p.addNewR();
        CTTextCharacterProperties rPr = r.addNewRPr();
        rPr.setLang("en-US");
        XSLFTextRun run = new XSLFTextRun(r, this);
        _runs.add(run);
        return run;
    }

    /**
     * Insert a line break
     *
     * @return text run representing this line break ('\n')
     */
    public XSLFTextRun addLineBreak(){
        CTTextLineBreak br = _p.addNewBr();
        CTTextCharacterProperties brProps = br.addNewRPr();
        if(_runs.size() > 0){
            // by default line break has the font size of the last text run
            CTTextCharacterProperties prevRun = _runs.get(_runs.size() - 1).getRPr();
            brProps.set(prevRun);
        }
        CTRegularTextRun r = CTRegularTextRun.Factory.newInstance();
        r.setRPr(brProps);
        r.setT("\n");
        XSLFTextRun run = new XSLFLineBreak(r, this, brProps);
        _runs.add(run);
        return run;
    }

    @Override
    public TextAlign getTextAlign(){
        ParagraphPropertyFetcher<TextAlign> fetcher = new ParagraphPropertyFetcher<TextAlign>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetAlgn()){
                    TextAlign val = TextAlign.values()[props.getAlgn().intValue() - 1];
                    setValue(val);
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    @Override
    public void setTextAlign(TextAlign align) {
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        if(align == null) {
            if(pr.isSetAlgn()) pr.unsetAlgn();
        } else {
            pr.setAlgn(STTextAlignType.Enum.forInt(align.ordinal() + 1));
        }
    }

    @Override
    public FontAlign getFontAlign(){
        ParagraphPropertyFetcher<FontAlign> fetcher = new ParagraphPropertyFetcher<FontAlign>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetFontAlgn()){
                    FontAlign val = FontAlign.values()[props.getFontAlgn().intValue() - 1];
                    setValue(val);
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    /**
     * Specifies the font alignment that is to be applied to the paragraph.
     * Possible values for this include auto, top, center, baseline and bottom.
     * see {@link org.apache.poi.sl.usermodel.TextParagraph.FontAlign}.
     *
     * @param align font align
     */
    public void setFontAlign(FontAlign align){
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        if(align == null) {
            if(pr.isSetFontAlgn()) pr.unsetFontAlgn();
        } else {
            pr.setFontAlgn(STTextFontAlignType.Enum.forInt(align.ordinal() + 1));
        }
    }

    

    /**
     * @return the font to be used on bullet characters within a given paragraph
     */
    public String getBulletFont(){
        ParagraphPropertyFetcher<String> fetcher = new ParagraphPropertyFetcher<String>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetBuFont()){
                    setValue(props.getBuFont().getTypeface());
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    public void setBulletFont(String typeface){
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        CTTextFont font = pr.isSetBuFont() ? pr.getBuFont() : pr.addNewBuFont();
        font.setTypeface(typeface);
    }

    /**
     * @return the character to be used in place of the standard bullet point
     */
    public String getBulletCharacter(){
        ParagraphPropertyFetcher<String> fetcher = new ParagraphPropertyFetcher<String>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetBuChar()){
                    setValue(props.getBuChar().getChar());
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    public void setBulletCharacter(String str){
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        CTTextCharBullet c = pr.isSetBuChar() ? pr.getBuChar() : pr.addNewBuChar();
        c.setChar(str);
    }

    /**
     *
     * @return the color of bullet characters within a given paragraph.
     * A <code>null</code> value means to use the text font color.
     */
    public PaintStyle getBulletFontColor(){
        final XSLFTheme theme = getParentShape().getSheet().getTheme();
        ParagraphPropertyFetcher<Color> fetcher = new ParagraphPropertyFetcher<Color>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetBuClr()){
                    XSLFColor c = new XSLFColor(props.getBuClr(), theme, null);
                    setValue(c.getColor());
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        Color col = fetcher.getValue();
        return (col == null) ? null : DrawPaint.createSolidPaint(col);
    }

    public void setBulletFontColor(Color color) {
        setBulletFontColor(DrawPaint.createSolidPaint(color));
    }
    
    
    /**
     * Set the color to be used on bullet characters within a given paragraph.
     *
     * @param color the bullet color
     */
    public void setBulletFontColor(PaintStyle color) {
        if (!(color instanceof SolidPaint)) {
            throw new IllegalArgumentException("Currently XSLF only supports SolidPaint");
        }

        // TODO: implement setting bullet color to null
        SolidPaint sp = (SolidPaint)color;
        Color col = DrawPaint.applyColorTransform(sp.getSolidColor());
        
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        CTColor c = pr.isSetBuClr() ? pr.getBuClr() : pr.addNewBuClr();
        CTSRgbColor clr = c.isSetSrgbClr() ? c.getSrgbClr() : c.addNewSrgbClr();
        clr.setVal(new byte[]{(byte) col.getRed(), (byte) col.getGreen(), (byte) col.getBlue()});
    }

    /**
     * Returns the bullet size that is to be used within a paragraph.
     * This may be specified in two different ways, percentage spacing and font point spacing:
     * <p>
     * If bulletSize >= 0, then bulletSize is a percentage of the font size.
     * If bulletSize < 0, then it specifies the size in points
     * </p>
     *
     * @return the bullet size
     */
    public Double getBulletFontSize(){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetBuSzPct()){
                    setValue(props.getBuSzPct().getVal() * 0.001);
                    return true;
                }
                if(props.isSetBuSzPts()){
                    setValue( - props.getBuSzPts().getVal() * 0.01);
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    /**
     * Sets the bullet size that is to be used within a paragraph.
     * This may be specified in two different ways, percentage spacing and font point spacing:
     * <p>
     * If bulletSize >= 0, then bulletSize is a percentage of the font size.
     * If bulletSize < 0, then it specifies the size in points
     * </p>
     */
    public void setBulletFontSize(double bulletSize){
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();

        if(bulletSize >= 0) {
            CTTextBulletSizePercent pt = pr.isSetBuSzPct() ? pr.getBuSzPct() : pr.addNewBuSzPct();
            pt.setVal((int)(bulletSize*1000));
            if(pr.isSetBuSzPts()) pr.unsetBuSzPts();
        } else {
            CTTextBulletSizePoint pt = pr.isSetBuSzPts() ? pr.getBuSzPts() : pr.addNewBuSzPts();
            pt.setVal((int)(-bulletSize*100));
            if(pr.isSetBuSzPct()) pr.unsetBuSzPct();
        }
   }

    /**
     * @return the auto numbering scheme, or null if not defined
     */
    public AutoNumberingScheme getAutoNumberingScheme() {
        ParagraphPropertyFetcher<AutoNumberingScheme> fetcher = new ParagraphPropertyFetcher<AutoNumberingScheme>(getIndentLevel()) {
            public boolean fetch(CTTextParagraphProperties props) {
                if (props.isSetBuAutoNum()) {
                    AutoNumberingScheme ans = AutoNumberingScheme.forOoxmlID(props.getBuAutoNum().getType().intValue());
                    if (ans != null) {
                        setValue(ans);
                        return true;
                    }
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    /**
     * @return the auto numbering starting number, or null if not defined
     */
    public Integer getAutoNumberingStartAt() {
        ParagraphPropertyFetcher<Integer> fetcher = new ParagraphPropertyFetcher<Integer>(getIndentLevel()) {
            public boolean fetch(CTTextParagraphProperties props) {
                if (props.isSetBuAutoNum()) {
                    if (props.getBuAutoNum().isSetStartAt()) {
                        setValue(props.getBuAutoNum().getStartAt());
                        return true;
                    }
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }
    
    
    @Override
    public void setIndent(Double indent){
        if ((indent == null) && !_p.isSetPPr()) return;
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        if(indent == null) {
            if(pr.isSetIndent()) pr.unsetIndent();
        } else {
            pr.setIndent(Units.toEMU(indent));
        }
    }

    @Override
    public Double getIndent() {

        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetIndent()){
                    setValue(Units.toPoints(props.getIndent()));
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);

        return fetcher.getValue();
    }

    @Override
    public void setLeftMargin(Double leftMargin){
        if (leftMargin == null && !_p.isSetPPr()) return;
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        if (leftMargin == null) {
            if(pr.isSetMarL()) pr.unsetMarL();
        } else {
            pr.setMarL(Units.toEMU(leftMargin));
        }

    }

    /**
     * @return the left margin (in points) of the paragraph, null if unset
     */
    @Override
    public Double getLeftMargin(){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetMarL()){
                    double val = Units.toPoints(props.getMarL());
                    setValue(val);
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        // if the marL attribute is omitted, then a value of 347663 is implied
        return fetcher.getValue();
    }

    @Override
    public void setRightMargin(Double rightMargin){
        if (rightMargin == null && !_p.isSetPPr()) return;
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        if(rightMargin == null) {
            if(pr.isSetMarR()) pr.unsetMarR();
        } else {
            pr.setMarR(Units.toEMU(rightMargin));
        }
    }

    /**
     *
     * @return the right margin of the paragraph, null if unset
     */
    @Override
    public Double getRightMargin(){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetMarR()){
                    double val = Units.toPoints(props.getMarR());
                    setValue(val);
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    @Override
    public Double getDefaultTabSize(){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetDefTabSz()){
                    double val = Units.toPoints(props.getDefTabSz());
                    setValue(val);
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    public double getTabStop(final int idx){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetTabLst()){
                    CTTextTabStopList tabStops = props.getTabLst();
                    if(idx < tabStops.sizeOfTabArray() ) {
                        CTTextTabStop ts = tabStops.getTabArray(idx);
                        double val = Units.toPoints(ts.getPos());
                        setValue(val);
                        return true;
                    }
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue() == null ? 0. : fetcher.getValue();
    }

    public void addTabStop(double value){
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        CTTextTabStopList tabStops = pr.isSetTabLst() ? pr.getTabLst() : pr.addNewTabLst();
        tabStops.addNewTab().setPos(Units.toEMU(value));
    }

    @Override
    public void setLineSpacing(Double lineSpacing){
        if (lineSpacing == null && !_p.isSetPPr()) return;
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        if(lineSpacing == null) {
            if (pr.isSetLnSpc()) pr.unsetLnSpc();
        } else {
            CTTextSpacing spc = (pr.isSetLnSpc()) ? pr.getLnSpc() : pr.addNewLnSpc();
            if (lineSpacing >= 0) {
                (spc.isSetSpcPct() ? spc.getSpcPct() : spc.addNewSpcPct()).setVal((int)(lineSpacing*1000));
                if (spc.isSetSpcPts()) spc.unsetSpcPts();
            } else {
                (spc.isSetSpcPts() ? spc.getSpcPts() : spc.addNewSpcPts()).setVal((int)(-lineSpacing*100));
                if (spc.isSetSpcPct()) spc.unsetSpcPct();
            }
        }
    }

    @Override
    public Double getLineSpacing(){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetLnSpc()){
                    CTTextSpacing spc = props.getLnSpc();

                    if(spc.isSetSpcPct()) setValue( spc.getSpcPct().getVal()*0.001 );
                    else if (spc.isSetSpcPts()) setValue( -spc.getSpcPts().getVal()*0.01 );
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);

        Double lnSpc = fetcher.getValue();
        if (lnSpc != null && lnSpc > 0) {
            // check if the percentage value is scaled
            CTTextNormalAutofit normAutofit = getParentShape().getTextBodyPr().getNormAutofit();
            if(normAutofit != null) {
                double scale = 1 - (double)normAutofit.getLnSpcReduction() / 100000;
                lnSpc *= scale;
            }
        }
        
        return lnSpc;
    }

    @Override
    public void setSpaceBefore(Double spaceBefore){
        if (spaceBefore == null && !_p.isSetPPr()) {
            return;
        }

        // unset the space before on null input
        if (spaceBefore == null) {
            if(_p.getPPr().isSetSpcBef()) {
                _p.getPPr().unsetSpcBef();
            }
            return;
        }

        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        CTTextSpacing spc = CTTextSpacing.Factory.newInstance();

        if(spaceBefore >= 0) {
            spc.addNewSpcPct().setVal((int)(spaceBefore*1000));
        } else {
            spc.addNewSpcPts().setVal((int)(-spaceBefore*100));
        }
        pr.setSpcBef(spc);
    }

    @Override
    public Double getSpaceBefore(){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetSpcBef()){
                    CTTextSpacing spc = props.getSpcBef();

                    if(spc.isSetSpcPct()) setValue( spc.getSpcPct().getVal()*0.001 );
                    else if (spc.isSetSpcPts()) setValue( -spc.getSpcPts().getVal()*0.01 );
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);

        return fetcher.getValue();
    }

    @Override
    public void setSpaceAfter(Double spaceAfter){
        if (spaceAfter == null && !_p.isSetPPr()) {
            return;
        }

        // unset the space before on null input
        if (spaceAfter == null) {
            if(_p.getPPr().isSetSpcAft()) {
                _p.getPPr().unsetSpcAft();
            }
            return;
        }

        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        CTTextSpacing spc = CTTextSpacing.Factory.newInstance();

        if(spaceAfter >= 0) {
            spc.addNewSpcPct().setVal((int)(spaceAfter*1000));
        } else {
            spc.addNewSpcPts().setVal((int)(-spaceAfter*100));
        }
        pr.setSpcAft(spc);
    }

    @Override
    public Double getSpaceAfter(){
        ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetSpcAft()){
                    CTTextSpacing spc = props.getSpcAft();

                    if(spc.isSetSpcPct()) setValue( spc.getSpcPct().getVal()*0.001 );
                    else if (spc.isSetSpcPts()) setValue( -spc.getSpcPts().getVal()*0.01 );
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue();
    }

    @Override
    public void setIndentLevel(int level){
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        pr.setLvl(level);
    }

    @Override
    public int getIndentLevel() {
        CTTextParagraphProperties pr = _p.getPPr();
        return (pr == null || !pr.isSetLvl()) ? 0 : pr.getLvl();
    }

    /**
     * Returns whether this paragraph has bullets
     */
    public boolean isBullet() {
        ParagraphPropertyFetcher<Boolean> fetcher = new ParagraphPropertyFetcher<Boolean>(getIndentLevel()){
            public boolean fetch(CTTextParagraphProperties props){
                if(props.isSetBuNone()) {
                    setValue(false);
                    return true;
                }
                if(props.isSetBuFont() || props.isSetBuChar()){
                    setValue(true);
                    return true;
                }
                return false;
            }
        };
        fetchParagraphProperty(fetcher);
        return fetcher.getValue() == null ? false : fetcher.getValue();
    }

    /**
     *
     * @param flag whether text in this paragraph has bullets
     */
    public void setBullet(boolean flag) {
        if(isBullet() == flag) return;

        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        if(flag) {
            pr.addNewBuFont().setTypeface("Arial");
            pr.addNewBuChar().setChar("\u2022");
        } else {
            if (pr.isSetBuFont()) pr.unsetBuFont();
            if (pr.isSetBuChar()) pr.unsetBuChar();
            if (pr.isSetBuAutoNum()) pr.unsetBuAutoNum();
            if (pr.isSetBuBlip()) pr.unsetBuBlip();
            if (pr.isSetBuClr()) pr.unsetBuClr();
            if (pr.isSetBuClrTx()) pr.unsetBuClrTx();
            if (pr.isSetBuFont()) pr.unsetBuFont();
            if (pr.isSetBuFontTx()) pr.unsetBuFontTx();
            if (pr.isSetBuSzPct()) pr.unsetBuSzPct();
            if (pr.isSetBuSzPts()) pr.unsetBuSzPts();
            if (pr.isSetBuSzTx()) pr.unsetBuSzTx();
            pr.addNewBuNone();
        }
    }

    /**
     * Specifies that automatic numbered bullet points should be applied to this paragraph
     *
     * @param scheme type of auto-numbering
     * @param startAt the number that will start number for a given sequence of automatically
    numbered bullets (1-based).
     */
    public void setBulletAutoNumber(AutoNumberingScheme scheme, int startAt) {
        if(startAt < 1) throw new IllegalArgumentException("Start Number must be greater or equal that 1") ;
        CTTextParagraphProperties pr = _p.isSetPPr() ? _p.getPPr() : _p.addNewPPr();
        CTTextAutonumberBullet lst = pr.isSetBuAutoNum() ? pr.getBuAutoNum() : pr.addNewBuAutoNum();
        lst.setType(STTextAutonumberScheme.Enum.forInt(scheme.ooxmlId));
        lst.setStartAt(startAt);
    }

    @Override
    public String toString(){
        return "[" + getClass() + "]" + getText();
    }


    /* package */ CTTextParagraphProperties getDefaultMasterStyle(){
        CTPlaceholder ph = _shape.getCTPlaceholder();
        String defaultStyleSelector;  
        switch(ph == null ? -1 : ph.getType().intValue()) {
            case STPlaceholderType.INT_TITLE:
            case STPlaceholderType.INT_CTR_TITLE:
                defaultStyleSelector = "titleStyle";
                break;
            case -1: // no placeholder means plain text box
            case STPlaceholderType.INT_FTR:
            case STPlaceholderType.INT_SLD_NUM:
            case STPlaceholderType.INT_DT:
                defaultStyleSelector = "otherStyle";
                break;
            default:
                defaultStyleSelector = "bodyStyle";
                break;
        }
        int level = getIndentLevel();

        // wind up and find the root master sheet which must be slide master
        final String nsDecl =
            "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' " +
            "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main' ";
        final String xpaths[] = {
            nsDecl+".//p:txStyles/p:" + defaultStyleSelector +"/a:lvl" +(level+1)+ "pPr",
            nsDecl+".//p:notesStyle/a:lvl" +(level+1)+ "pPr"
        };
        XSLFSheet masterSheet = _shape.getSheet();
        for (XSLFSheet m = masterSheet; m != null; m = (XSLFSheet)m.getMasterSheet()) {
            masterSheet = m;
            XmlObject xo = masterSheet.getXmlObject();
            for (String xpath : xpaths) {
                XmlObject[] o = xo.selectPath(xpath);
                if (o.length == 1) {
                    return (CTTextParagraphProperties)o[0];
                }
            }
        }

        
//        for (CTTextBody txBody : (CTTextBody[])xo.selectPath(nsDecl+".//p:txBody")) {
//            CTTextParagraphProperties defaultPr = null, lastPr = null;
//            boolean hasLvl = false;
//            for (CTTextParagraph p : txBody.getPArray()) {
//                CTTextParagraphProperties pr = p.getPPr();
//                if (pr.isSetLvl()) {
//                    hasLvl |= true;
//                    lastPr = pr;
//                    if (pr.getLvl() == level) return pr;
//                } else {
//                    defaultPr = pr;
//                }
//            }
//            if (!hasLvl) continue;
//            if (level == 0 && defaultPr != null) return defaultPr;
//            if (lastPr != null) return lastPr;
//            break;
//        }
//           
//        String err = "Failed to fetch default style for " + defaultStyleSelector + " and level=" + level;
//        throw new IllegalArgumentException(err);
        
        return null;
    }

    private <T> boolean fetchParagraphProperty(ParagraphPropertyFetcher<T> visitor){
        boolean ok = false;
        XSLFTextShape shape = getParentShape();
        XSLFSheet sheet = shape.getSheet();
        
        if(_p.isSetPPr()) ok = visitor.fetch(_p.getPPr());
        if (ok) return true;

        ok = shape.fetchShapeProperty(visitor);
        if (ok) return true;
                
        
        CTPlaceholder ph = shape.getCTPlaceholder();
        if(ph == null){
            // if it is a plain text box then take defaults from presentation.xml
            @SuppressWarnings("resource")
            XMLSlideShow ppt = sheet.getSlideShow();
            CTTextParagraphProperties themeProps = ppt.getDefaultParagraphStyle(getIndentLevel());
            if (themeProps != null) ok = visitor.fetch(themeProps);
        }
        if (ok) return true;

        // defaults for placeholders are defined in the slide master
        CTTextParagraphProperties defaultProps = getDefaultMasterStyle();
        // TODO: determine master shape
        if(defaultProps != null) ok = visitor.fetch(defaultProps);
        if (ok) return true;

        return false;
    }

    void copy(XSLFTextParagraph other){
        if (other == this) return;
        
        CTTextParagraph thisP = getXmlObject();
        CTTextParagraph otherP = other.getXmlObject();
        
        if (thisP.isSetPPr()) thisP.unsetPPr();
        if (thisP.isSetEndParaRPr()) thisP.unsetEndParaRPr();
        
        _runs.clear();
        for (int i=thisP.sizeOfBrArray(); i>0; i--) {
            thisP.removeBr(i-1);
        }
        for (int i=thisP.sizeOfRArray(); i>0; i--) {
            thisP.removeR(i-1);
        }
        for (int i=thisP.sizeOfFldArray(); i>0; i--) {
            thisP.removeFld(i-1);
        }

        XmlCursor thisC = thisP.newCursor();
        thisC.toEndToken();
        XmlCursor otherC = otherP.newCursor();
        otherC.copyXmlContents(thisC);
        otherC.dispose();
        thisC.dispose();
        
        List<XSLFTextRun> otherRs = other.getTextRuns();
        int i=0;
        for(CTRegularTextRun rtr : thisP.getRArray()) {
            XSLFTextRun run = new XSLFTextRun(rtr, this);
            run.copy(otherRs.get(i++));
            _runs.add(run);
        }
        
        
        // set properties again, in case we are based on a different
        // template
        TextAlign srcAlign = other.getTextAlign();
        if(srcAlign != getTextAlign()){
            setTextAlign(srcAlign);
        }

        boolean isBullet = other.isBullet();
        if(isBullet != isBullet()){
            setBullet(isBullet);
            if(isBullet) {
                String buFont = other.getBulletFont();
                if(buFont != null && !buFont.equals(getBulletFont())){
                    setBulletFont(buFont);
                }
                String buChar = other.getBulletCharacter();
                if(buChar != null && !buChar.equals(getBulletCharacter())){
                    setBulletCharacter(buChar);
                }
                PaintStyle buColor = other.getBulletFontColor();
                if(buColor != null && !buColor.equals(getBulletFontColor())){
                    setBulletFontColor(buColor);
                }
                Double buSize = other.getBulletFontSize();
                if(!doubleEquals(buSize, getBulletFontSize())){
                    setBulletFontSize(buSize);
                }
            }
        }

        Double leftMargin = other.getLeftMargin();
        if (!doubleEquals(leftMargin, getLeftMargin())){
            setLeftMargin(leftMargin);
        }

        Double indent = other.getIndent();
        if (!doubleEquals(indent, getIndent())) {
            setIndent(indent);
        }

        Double spaceAfter = other.getSpaceAfter();
        if (!doubleEquals(spaceAfter, getSpaceAfter())) {
            setSpaceAfter(spaceAfter);
        }
        
        Double spaceBefore = other.getSpaceBefore();
        if (!doubleEquals(spaceBefore, getSpaceBefore())) {
            setSpaceBefore(spaceBefore);
        }
        
        Double lineSpacing = other.getLineSpacing();
        if (!doubleEquals(lineSpacing, getLineSpacing())) {
            setLineSpacing(lineSpacing);
        }
    }

    private static boolean doubleEquals(Double d1, Double d2) {
        return (d1 == d2 || (d1 != null && d1.equals(d2)));
    }
    
    @Override
    public Double getDefaultFontSize() {
        CTTextCharacterProperties endPr = _p.getEndParaRPr();
        return (endPr == null || !endPr.isSetSz()) ? 12 : (endPr.getSz() / 100.);
    }

    @Override
    public String getDefaultFontFamily() {
        return (_runs.isEmpty() ? "Arial" : _runs.get(0).getFontFamily());
    }

    @Override
    public BulletStyle getBulletStyle() {
        if (!isBullet()) return null;
        return new BulletStyle(){
            @Override
            public String getBulletCharacter() {
                return XSLFTextParagraph.this.getBulletCharacter();
            }

            @Override
            public String getBulletFont() {
                return XSLFTextParagraph.this.getBulletFont();
            }

            @Override
            public Double getBulletFontSize() {
                return XSLFTextParagraph.this.getBulletFontSize();
            }

            @Override
            public PaintStyle getBulletFontColor() {
                return XSLFTextParagraph.this.getBulletFontColor();
            }
            
            @Override
            public void setBulletFontColor(Color color) {
                setBulletFontColor(DrawPaint.createSolidPaint(color));
            }

            @Override
            public void setBulletFontColor(PaintStyle color) {
                XSLFTextParagraph.this.setBulletFontColor(color);
            }
            
            @Override
            public AutoNumberingScheme getAutoNumberingScheme() {
                return XSLFTextParagraph.this.getAutoNumberingScheme();
            }

            @Override
            public Integer getAutoNumberingStartAt() {
                return XSLFTextParagraph.this.getAutoNumberingStartAt();
            }

        };
    }

    @Override
    public void setBulletStyle(Object... styles) {
        if (styles.length == 0) {
            setBullet(false);
        } else {
            setBullet(true);
            for (Object ostyle : styles) {
                if (ostyle instanceof Number) {
                    setBulletFontSize(((Number)ostyle).doubleValue());
                } else if (ostyle instanceof Color) {
                    setBulletFontColor((Color)ostyle);
                } else if (ostyle instanceof Character) {
                    setBulletCharacter(ostyle.toString());
                } else if (ostyle instanceof String) {
                    setBulletFont((String)ostyle);
                } else if (ostyle instanceof AutoNumberingScheme) {
                    setBulletAutoNumber((AutoNumberingScheme)ostyle, 0);
                }
            }
        }
    }
    
    /**
     * Helper method for appending text and keeping paragraph and character properties.
     * The character properties are moved to the end paragraph marker
     */
    /* package */ void clearButKeepProperties() {
        CTTextParagraph thisP = getXmlObject();
        for (int i=thisP.sizeOfBrArray(); i>0; i--) {
            thisP.removeBr(i-1);
        }
        for (int i=thisP.sizeOfFldArray(); i>0; i--) {
            thisP.removeFld(i-1);
        }
        if (!_runs.isEmpty()) {
            int size = _runs.size();
            thisP.setEndParaRPr(_runs.get(size-1).getRPr());
            for (int i=size; i>0; i--) {
                thisP.removeR(i-1);
            }
            _runs.clear();
        }
    }

    @Override
    public boolean isHeaderOrFooter() {
        CTPlaceholder ph = _shape.getCTPlaceholder();
        int phId = (ph == null ? -1 : ph.getType().intValue());
        switch (phId) {
            case STPlaceholderType.INT_SLD_NUM:
            case STPlaceholderType.INT_DT:
            case STPlaceholderType.INT_FTR:
            case STPlaceholderType.INT_HDR:
                return true;
            default:
                return false;
        }
    }
}