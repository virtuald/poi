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

package org.apache.poi.xssf.streaming;

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;

/**
 * SXSSF wrapper for a cell under evaluation
 */
final class SXSSFEvaluationCell implements EvaluationCell {
    private final EvaluationSheet _evalSheet;
    private final SXSSFCell _cell;

    public SXSSFEvaluationCell(SXSSFCell cell, SXSSFEvaluationSheet evaluationSheet) {
        _cell = cell;
        _evalSheet = evaluationSheet;
    }

    public SXSSFEvaluationCell(SXSSFCell cell) {
        this(cell, new SXSSFEvaluationSheet(cell.getSheet()));
    }

    public Object getIdentityKey() {
        // save memory by just using the cell itself as the identity key
        // Note - this assumes SXSSFCell has not overridden hashCode and equals
        return _cell;
    }

    public SXSSFCell getSXSSFCell() {
        return _cell;
    }
    public boolean getBooleanCellValue() {
        return _cell.getBooleanCellValue();
    }
    public int getCellType() {
        return _cell.getCellType();
    }
    public int getColumnIndex() {
        return _cell.getColumnIndex();
    }
    public int getErrorCellValue() {
        return _cell.getErrorCellValue();
    }
    public double getNumericCellValue() {
        return _cell.getNumericCellValue();
    }
    public int getRowIndex() {
        return _cell.getRowIndex();
    }
    public EvaluationSheet getSheet() {
        return _evalSheet;
    }
    public String getStringCellValue() {
        return _cell.getRichStringCellValue().getString();
    }
    public int getCachedFormulaResultType() {
        return _cell.getCachedFormulaResultType();
    }
}
