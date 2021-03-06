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

package org.apache.poi.ss.formula;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.poi.hssf.HSSFTestDataSamples;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.MissingArgEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ptg.AreaErrPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.DeletedArea3DPtg;
import org.apache.poi.ss.formula.ptg.DeletedRef3DPtg;
import org.apache.poi.ss.formula.ptg.IntPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefErrorPtg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests {@link WorkbookEvaluator}.
 *
 * @author Josh Micich
 */
public class TestWorkbookEvaluator {

    private static final double EPSILON = 0.0000001;

    private static ValueEval evaluateFormula(Ptg[] ptgs) {
        OperationEvaluationContext ec = new OperationEvaluationContext(null, null, 0, 0, 0, null);
        return new WorkbookEvaluator(null, null, null).evaluateFormula(ec, ptgs);
    }

    /**
     * Make sure that the evaluator can directly handle tAttrSum (instead of relying on re-parsing
     * the whole formula which converts tAttrSum to tFuncVar("SUM") )
     */
    @Test
    public void testAttrSum() {

        Ptg[] ptgs = {
            new IntPtg(42),
            AttrPtg.SUM,
        };

        ValueEval result = evaluateFormula(ptgs);
        assertEquals(42, ((NumberEval)result).getNumberValue(), 0.0);
    }

    /**
     * Make sure that the evaluator can directly handle (deleted) ref error tokens
     * (instead of relying on re-parsing the whole formula which converts these
     * to the error constant #REF! )
     */
    @Test
    public void testRefErr() {

        confirmRefErr(new RefErrorPtg());
        confirmRefErr(new AreaErrPtg());
        confirmRefErr(new DeletedRef3DPtg(0));
        confirmRefErr(new DeletedArea3DPtg(0));
    }
    private static void confirmRefErr(Ptg ptg) {
        Ptg[] ptgs = {
            ptg,
        };

        ValueEval result = evaluateFormula(ptgs);
        assertEquals(ErrorEval.REF_INVALID, result);
    }

    /**
     * Make sure that the evaluator can directly handle tAttrSum (instead of relying on re-parsing
     * the whole formula which converts tAttrSum to tFuncVar("SUM") )
     */
    @Test
    public void testMemFunc() {

        Ptg[] ptgs = {
            new IntPtg(42),
            AttrPtg.SUM,
        };

        ValueEval result = evaluateFormula(ptgs);
        assertEquals(42, ((NumberEval)result).getNumberValue(), 0.0);
    }


    @Test
    public void testEvaluateMultipleWorkbooks() {
        HSSFWorkbook wbA = HSSFTestDataSamples.openSampleWorkbook("multibookFormulaA.xls");
        HSSFWorkbook wbB = HSSFTestDataSamples.openSampleWorkbook("multibookFormulaB.xls");

        HSSFFormulaEvaluator evaluatorA = new HSSFFormulaEvaluator(wbA);
        HSSFFormulaEvaluator evaluatorB = new HSSFFormulaEvaluator(wbB);

        // Hook up the workbook evaluators to enable evaluation of formulas across books
        String[] bookNames = { "multibookFormulaA.xls", "multibookFormulaB.xls", };
        HSSFFormulaEvaluator[] evaluators = { evaluatorA, evaluatorB, };
        HSSFFormulaEvaluator.setupEnvironment(bookNames, evaluators);

        HSSFCell cell;

        HSSFSheet aSheet1 = wbA.getSheetAt(0);
        HSSFSheet bSheet1 = wbB.getSheetAt(0);

        // Simple case - single link from wbA to wbB
        confirmFormula(wbA, 0, 0, 0, "[multibookFormulaB.xls]BSheet1!B1");
        cell = aSheet1.getRow(0).getCell(0);
        confirmEvaluation(35, evaluatorA, cell);


        // more complex case - back link into wbA
        // [wbA]ASheet1!A2 references (among other things) [wbB]BSheet1!B2
        confirmFormula(wbA, 0, 1, 0, "[multibookFormulaB.xls]BSheet1!$B$2+2*A3");
        // [wbB]BSheet1!B2 references (among other things) [wbA]AnotherSheet!A1:B2
        confirmFormula(wbB, 0, 1, 1, "SUM([multibookFormulaA.xls]AnotherSheet!$A$1:$B$2)+B3");

        cell = aSheet1.getRow(1).getCell(0);
        confirmEvaluation(264, evaluatorA, cell);

        // change [wbB]BSheet1!B3 (from 50 to 60)
        HSSFCell cellB3 = bSheet1.getRow(2).getCell(1);
        cellB3.setCellValue(60);
        evaluatorB.notifyUpdateCell(cellB3);
        confirmEvaluation(274, evaluatorA, cell);

        // change [wbA]ASheet1!A3 (from 100 to 80)
        HSSFCell cellA3 = aSheet1.getRow(2).getCell(0);
        cellA3.setCellValue(80);
        evaluatorA.notifyUpdateCell(cellA3);
        confirmEvaluation(234, evaluatorA, cell);

        // change [wbA]AnotherSheet!A1 (from 2 to 3)
        HSSFCell cellA1 = wbA.getSheetAt(1).getRow(0).getCell(0);
        cellA1.setCellValue(3);
        evaluatorA.notifyUpdateCell(cellA1);
        confirmEvaluation(235, evaluatorA, cell);
    }

    private static void confirmEvaluation(double expectedValue, HSSFFormulaEvaluator fe, HSSFCell cell) {
        assertEquals(expectedValue, fe.evaluate(cell).getNumberValue(), 0.0);
    }

    private static void confirmFormula(HSSFWorkbook wb, int sheetIndex, int rowIndex, int columnIndex,
            String expectedFormula) {
        HSSFCell cell = wb.getSheetAt(sheetIndex).getRow(rowIndex).getCell(columnIndex);
        assertEquals(expectedFormula, cell.getCellFormula());
    }

    /**
     * This test makes sure that any {@link MissingArgEval} that propagates to
     * the result of a function gets translated to {@link BlankEval}.
     */
    @Test
    public void testMissingArg() {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("Sheet1");
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        cell.setCellFormula("1+IF(1,,)");
        HSSFFormulaEvaluator fe = new HSSFFormulaEvaluator(wb);
        CellValue cv = null;
        try {
            cv = fe.evaluate(cell);
        } catch (RuntimeException e) {
            fail("Missing arg result not being handled correctly.");
        }
        assertEquals(HSSFCell.CELL_TYPE_NUMERIC, cv.getCellType());
        // adding blank to 1.0 gives 1.0
        assertEquals(1.0, cv.getNumberValue(), 0.0);

        // check with string operand
        cell.setCellFormula("\"abc\"&IF(1,,)");
        fe.notifySetFormula(cell);
        cv = fe.evaluate(cell);
        assertEquals(HSSFCell.CELL_TYPE_STRING, cv.getCellType());
        // adding blank to "abc" gives "abc"
        assertEquals("abc", cv.getStringValue());

        // check CHOOSE()
        cell.setCellFormula("\"abc\"&CHOOSE(2,5,,9)");
        fe.notifySetFormula(cell);
        cv = fe.evaluate(cell);
        assertEquals(HSSFCell.CELL_TYPE_STRING, cv.getCellType());
        // adding blank to "abc" gives "abc"
        assertEquals("abc", cv.getStringValue());
    }

    /**
     * Functions like IF, INDIRECT, INDEX, OFFSET etc can return AreaEvals which
     * should be dereferenced by the evaluator
     * @throws IOException 
     */
    @Test
    public void testResultOutsideRange() throws IOException {
        Workbook wb = new HSSFWorkbook();
        try {
            Cell cell = wb.createSheet("Sheet1").createRow(0).createCell(0);
            cell.setCellFormula("D2:D5"); // IF(TRUE,D2:D5,D2) or  OFFSET(D2:D5,0,0) would work too
            FormulaEvaluator fe = wb.getCreationHelper().createFormulaEvaluator();
            CellValue cv;
            try {
                cv = fe.evaluate(cell);
            } catch (IllegalArgumentException e) {
                if ("Specified row index (0) is outside the allowed range (1..4)".equals(e.getMessage())) {
                    fail("Identified bug in result dereferencing");
                }
                throw new RuntimeException(e);
            }
            assertEquals(Cell.CELL_TYPE_ERROR, cv.getCellType());
            assertEquals(ErrorEval.VALUE_INVALID.getErrorCode(), cv.getErrorValue());

            // verify circular refs are still detected properly
            fe.clearAllCachedResultValues();
            cell.setCellFormula("OFFSET(A1,0,0)");
            cv = fe.evaluate(cell);
            assertEquals(Cell.CELL_TYPE_ERROR, cv.getCellType());
            assertEquals(ErrorEval.CIRCULAR_REF_ERROR.getErrorCode(), cv.getErrorValue());
        } finally {
            wb.close();
        }
    }


    /**
     * formulas with defined names.
     * @throws IOException 
     */
    @Test
    public void testNamesInFormulas() throws IOException {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");
        
        Name name1 = wb.createName();
        name1.setNameName("aConstant");
        name1.setRefersToFormula("3.14");

        Name name2 = wb.createName();
        name2.setNameName("aFormula");
        name2.setRefersToFormula("SUM(Sheet1!$A$1:$A$3)");

        Name name3 = wb.createName();
        name3.setNameName("aSet");
        name3.setRefersToFormula("Sheet1!$A$2:$A$4");

        
        Row row0 = sheet.createRow(0);
        Row row1 = sheet.createRow(1);
        Row row2 = sheet.createRow(2);
        Row row3 = sheet.createRow(3);
        row0.createCell(0).setCellValue(2);
        row1.createCell(0).setCellValue(5);
        row2.createCell(0).setCellValue(3);
        row3.createCell(0).setCellValue(7);
        
        row0.createCell(2).setCellFormula("aConstant");
        row1.createCell(2).setCellFormula("aFormula");
        row2.createCell(2).setCellFormula("SUM(aSet)");
        row3.createCell(2).setCellFormula("aConstant+aFormula+SUM(aSet)");

        FormulaEvaluator fe = wb.getCreationHelper().createFormulaEvaluator();
        assertEquals(3.14, fe.evaluate(row0.getCell(2)).getNumberValue(), EPSILON);
        assertEquals(10.0, fe.evaluate(row1.getCell(2)).getNumberValue(), EPSILON);
        assertEquals(15.0, fe.evaluate(row2.getCell(2)).getNumberValue(), EPSILON);
        assertEquals(28.14, fe.evaluate(row3.getCell(2)).getNumberValue(), EPSILON);
        
        wb.close();
    }
    
// Test IF-Equals Formula Evaluation (bug 58591)
    
    private Workbook testIFEqualsFormulaEvaluation_setup(String formula, int a1CellType) {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("IFEquals");
        Row row = sheet.createRow(0);
        Cell A1 = row.createCell(0);
        Cell B1 = row.createCell(1);
        Cell C1 = row.createCell(2);
        Cell D1 = row.createCell(3);
        
        switch (a1CellType) {
            case Cell.CELL_TYPE_NUMERIC:
                A1.setCellValue(1.0);
                // "A1=1" should return true
                break;
            case Cell.CELL_TYPE_STRING:
                A1.setCellValue("1");
                // "A1=1" should return false
                // "A1=\"1\"" should return true
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                A1.setCellValue(true);
                // "A1=1" should return true
                break;
            case Cell.CELL_TYPE_FORMULA:
                A1.setCellFormula("1");
                // "A1=1" should return true
                break;
            case Cell.CELL_TYPE_BLANK:
                A1.setCellValue((String) null);
                // "A1=1" should return false
                break;
        }
        B1.setCellValue(2.0);
        C1.setCellValue(3.0);
        D1.setCellFormula(formula);
        
        return wb;
    }
    
    private void testIFEqualsFormulaEvaluation_teardown(Workbook wb) {
        try {
            wb.close();
        } catch (final IOException e) {
            fail("Unable to close workbook");
        }
    }
    
    
    
    private void testIFEqualsFormulaEvaluation_evaluate(
        String formula, int cellType, String expectedFormula, double expectedResult) {
        Workbook wb = testIFEqualsFormulaEvaluation_setup(formula, cellType);
        Cell D1 = wb.getSheet("IFEquals").getRow(0).getCell(3);
        
        FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
        CellValue result = eval.evaluate(D1);
        
        // Call should not modify the contents
        assertEquals(Cell.CELL_TYPE_FORMULA, D1.getCellType());
        assertEquals(expectedFormula, D1.getCellFormula());
        
        assertEquals(Cell.CELL_TYPE_NUMERIC, result.getCellType());
        assertEquals(expectedResult, result.getNumberValue(), EPSILON);
        
        testIFEqualsFormulaEvaluation_teardown(wb);
    }
    
    private void testIFEqualsFormulaEvaluation_eval(
            final String formula, final int cellType, final String expectedFormula, final double expectedValue) {
        testIFEqualsFormulaEvaluation_evaluate(formula, cellType, expectedFormula, expectedValue);
        testIFEqualsFormulaEvaluation_evaluateFormulaCell(formula, cellType, expectedFormula, expectedValue);
        testIFEqualsFormulaEvaluation_evaluateInCell(formula, cellType, expectedFormula, expectedValue);
        testIFEqualsFormulaEvaluation_evaluateAll(formula, cellType, expectedFormula, expectedValue);
        testIFEqualsFormulaEvaluation_evaluateAllFormulaCells(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_NumericLiteral() {
        final String formula = "IF(A1=1, 2, 3)";
        final int cellType = Cell.CELL_TYPE_NUMERIC;
        final String expectedFormula = "IF(A1=1,2,3)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_Numeric() {
        final String formula = "IF(A1=1, B1, C1)";
        final int cellType = Cell.CELL_TYPE_NUMERIC;
        final String expectedFormula = "IF(A1=1,B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_NumericCoerceToString() {
        final String formula = "IF(A1&\"\"=\"1\", B1, C1)";
        final int cellType = Cell.CELL_TYPE_NUMERIC;
        final String expectedFormula = "IF(A1&\"\"=\"1\",B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_String() {
        final String formula = "IF(A1=1, B1, C1)";
        final int cellType = Cell.CELL_TYPE_STRING;
        final String expectedFormula = "IF(A1=1,B1,C1)";
        final double expectedValue = 3.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_StringCompareToString() {
        final String formula = "IF(A1=\"1\", B1, C1)";
        final int cellType = Cell.CELL_TYPE_STRING;
        final String expectedFormula = "IF(A1=\"1\",B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_StringCoerceToNumeric() {
        final String formula = "IF(A1+0=1, B1, C1)";
        final int cellType = Cell.CELL_TYPE_STRING;
        final String expectedFormula = "IF(A1+0=1,B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Ignore("Bug 58591: this test currently fails")
    @Test
    public void testIFEqualsFormulaEvaluation_Boolean() {
        final String formula = "IF(A1=1, B1, C1)";
        final int cellType = Cell.CELL_TYPE_BOOLEAN;
        final String expectedFormula = "IF(A1=1,B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Ignore("Bug 58591: this test currently fails")
    @Test
    public void testIFEqualsFormulaEvaluation_BooleanSimple() {
        final String formula = "3-(A1=1)";
        final int cellType = Cell.CELL_TYPE_BOOLEAN;
        final String expectedFormula = "3-(A1=1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_Formula() {
        final String formula = "IF(A1=1, B1, C1)";
        final int cellType = Cell.CELL_TYPE_FORMULA;
        final String expectedFormula = "IF(A1=1,B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_Blank() {
        final String formula = "IF(A1=1, B1, C1)";
        final int cellType = Cell.CELL_TYPE_BLANK;
        final String expectedFormula = "IF(A1=1,B1,C1)";
        final double expectedValue = 3.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Test
    public void testIFEqualsFormulaEvaluation_BlankCompareToZero() {
        final String formula = "IF(A1=0, B1, C1)";
        final int cellType = Cell.CELL_TYPE_BLANK;
        final String expectedFormula = "IF(A1=0,B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Ignore("Bug 58591: this test currently fails")
    @Test
    public void testIFEqualsFormulaEvaluation_BlankInverted() {
        final String formula = "IF(NOT(A1)=1, B1, C1)";
        final int cellType = Cell.CELL_TYPE_BLANK;
        final String expectedFormula = "IF(NOT(A1)=1,B1,C1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    @Ignore("Bug 58591: this test currently fails")
    @Test
    public void testIFEqualsFormulaEvaluation_BlankInvertedSimple() {
        final String formula = "3-(NOT(A1)=1)";
        final int cellType = Cell.CELL_TYPE_BLANK;
        final String expectedFormula = "3-(NOT(A1)=1)";
        final double expectedValue = 2.0;
        testIFEqualsFormulaEvaluation_eval(formula, cellType, expectedFormula, expectedValue);
    }
    
    
    private void testIFEqualsFormulaEvaluation_evaluateFormulaCell(
            String formula, int cellType, String expectedFormula, double expectedResult) {
        Workbook wb = testIFEqualsFormulaEvaluation_setup(formula, cellType);
        Cell D1 = wb.getSheet("IFEquals").getRow(0).getCell(3);
        
        FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
        int resultCellType = eval.evaluateFormulaCell(D1);
        
        // Call should modify the contents, but leave the formula intact
        assertEquals(Cell.CELL_TYPE_FORMULA, D1.getCellType());
        assertEquals(expectedFormula, D1.getCellFormula());
        assertEquals(Cell.CELL_TYPE_NUMERIC, resultCellType);
        assertEquals(Cell.CELL_TYPE_NUMERIC, D1.getCachedFormulaResultType());
        assertEquals(expectedResult, D1.getNumericCellValue(), EPSILON);
        
        testIFEqualsFormulaEvaluation_teardown(wb);
    }
    
    private void testIFEqualsFormulaEvaluation_evaluateInCell(
            String formula, int cellType, String expectedFormula, double expectedResult) {
        Workbook wb = testIFEqualsFormulaEvaluation_setup(formula, cellType);
        Cell D1 = wb.getSheet("IFEquals").getRow(0).getCell(3);
        
        FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
        Cell result = eval.evaluateInCell(D1);
        
        // Call should modify the contents and replace the formula with the result
        assertSame(D1, result); // returns the same cell that was provided as an argument so that calls can be chained.
        try {
            D1.getCellFormula();
            fail("cell formula should be overwritten with formula result");
        } catch (final IllegalStateException expected) { }
        assertEquals(Cell.CELL_TYPE_NUMERIC, D1.getCellType());
        assertEquals(expectedResult, D1.getNumericCellValue(), EPSILON);
        
        testIFEqualsFormulaEvaluation_teardown(wb);
    }
    
    private void testIFEqualsFormulaEvaluation_evaluateAll(
            String formula, int cellType, String expectedFormula, double expectedResult) {
        Workbook wb = testIFEqualsFormulaEvaluation_setup(formula, cellType);
        Cell D1 = wb.getSheet("IFEquals").getRow(0).getCell(3);
        
        FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
        eval.evaluateAll();
        
        // Call should modify the contents
        assertEquals(Cell.CELL_TYPE_FORMULA, D1.getCellType());
        assertEquals(expectedFormula, D1.getCellFormula());
        
        assertEquals(Cell.CELL_TYPE_NUMERIC, D1.getCachedFormulaResultType());
        assertEquals(expectedResult, D1.getNumericCellValue(), EPSILON);
        
        testIFEqualsFormulaEvaluation_teardown(wb);
    }
    
    private void testIFEqualsFormulaEvaluation_evaluateAllFormulaCells(
            String formula, int cellType, String expectedFormula, double expectedResult) {
        Workbook wb = testIFEqualsFormulaEvaluation_setup(formula, cellType);
        Cell D1 = wb.getSheet("IFEquals").getRow(0).getCell(3);
        
        HSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
        
        // Call should modify the contents
        assertEquals(Cell.CELL_TYPE_FORMULA, D1.getCellType());
        // whitespace gets deleted because formula is parsed and re-rendered
        assertEquals(expectedFormula, D1.getCellFormula());
        
        assertEquals(Cell.CELL_TYPE_NUMERIC, D1.getCachedFormulaResultType());
        assertEquals(expectedResult, D1.getNumericCellValue(), EPSILON);
        
        testIFEqualsFormulaEvaluation_teardown(wb);
    }
}
