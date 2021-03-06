/*
 * -------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.data.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;
import org.junit.Assume;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.ObjectToDataCellConverter;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.DuplicateKeyException;

/**
 * Test case for class <code>DataContainer</code>.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("deprecation")
public class DataContainerTest extends TestCase {

    private static final DataTableSpec EMPTY_SPEC = new DataTableSpec(
            new String[] {}, new DataType[] {});
    private static final DataTableSpec SPEC_STR_INT_DBL = new DataTableSpec(new String[] {"String", "Int", "Double"},
            new DataType[] {StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE});

    /**
     * Main method. Ignores argument.
     *
     * @param args Ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(DataContainerTest.class);
    }

    /**
     * method being tested: open().
     */
    public final void testOpen() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        c.addRowToTable(new DefaultRow(
                "no one is going to read me", new DataCell[] {}));
        assertTrue(c.isOpen());
    }

    /**
     * method being tested: isClosed().
     */
    public final void testIsClosed() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        assertFalse(c.isClosed());
        c.close();
        assertTrue(c.isClosed());
        for (DataRow row : c.getTable()) {
            fail("No data should be in the table: " + row);
        }
    }

    /**
     * method being tested: close().
     */
    public final void testClose() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        c.close();
        // hm, does it work again?
        c.close(); // should ignore it
    }

    public final void testMemoryAlertAfterClose() throws Exception {
        DataContainer container = new DataContainer(SPEC_STR_INT_DBL,
                true, Integer.MAX_VALUE, false);
        for (RowIterator it = generateRows(100000); it.hasNext();) {
            container.addRowToTable(it.next());
        }
        container.close();
        Buffer buffer = container.getBufferedTable().getBuffer();
        MemoryAlertSystem.getInstance().sendMemoryAlert();
        RowIterator tableIterator = container.getTable().iterator();
        for (RowIterator it = generateRows(100000); it.hasNext();) {
            assertEquals(it.next(), tableIterator.next());
        }
    }

    public final void testMemoryAlertAfterCloseWhileReading() throws Exception {
        DataContainer container = new DataContainer(SPEC_STR_INT_DBL,
                true, Integer.MAX_VALUE, false);
        int count = 100000;
        for (RowIterator it = generateRows(count); it.hasNext();) {
            container.addRowToTable(it.next());
        }
        container.close();
        RowIterator tableIterator = container.getTable().iterator();
        RowIterator it = generateRows(count);
        int i;
        for (i = 0; i < count / 2; i++) {
            assertEquals(it.next(), tableIterator.next());
        }
        Buffer buffer = container.getBufferedTable().getBuffer();
        synchronized (buffer) {
            buffer.writeAllRowsFromListToFile();
        }

        for (; i < count; i++) {
            assertEquals(it.next(), tableIterator.next());
        }
    }

    public void testMemoryAlertWhileWrite() throws Exception {
        DataContainer cont = new DataContainer(SPEC_STR_INT_DBL, true, 1000000);
        int nrRows = 10;
        RowIterator it = generateRows(nrRows);
        int i = 0;
        for (; i < nrRows / 2; i++) {
            cont.addRowToTable(it.next());
        }
        Buffer buffer = cont.getBuffer();
        synchronized (buffer) {
            buffer.writeAllRowsFromListToFile();
        }
        for (; i < nrRows; i++) {
            cont.addRowToTable(it.next());
        }
        cont.close();
        RowIterator tableIT = cont.getTable().iterator();
        for (RowIterator r = generateRows(nrRows); r.hasNext();) {
            DataRow expected = r.next();
            DataRow actual = tableIT.next();
            assertEquals(expected, actual);
        }
    }

    public final void testMemoryAlertWhileRestore() throws Exception {
        DataContainer container = new DataContainer(SPEC_STR_INT_DBL, true, /* no rows in mem */ 0, false);
        int count = 100000;
        for (RowIterator it = generateRows(count); it.hasNext();) {
            container.addRowToTable(it.next());
        }
        container.close();
        final Buffer buffer = container.getBufferedTable().getBuffer();
        assertTrue(buffer.usesOutFile());
        buffer.restoreIntoMemory();
        RowIterator tableIterator1 = container.getTable().iterator();
        RowIterator tableIterator2 = container.getTable().iterator();
        RowIterator referenceIterator = generateRows(count);
        int i;
        for (i = 0; i < count; i++) {
            if (i == count / 2) {
                synchronized (buffer) {
                    // currently it does nothing as memory alerts while restoring is not supported
                    MemoryAlertSystem.getInstance().sendMemoryAlert();
                }
            }
            RowIterator pushIterator, otherIterator;
            if (i % 2 == 0) {
                pushIterator = tableIterator1;
                otherIterator = tableIterator2;
            } else {
                pushIterator = tableIterator2;
                otherIterator = tableIterator1;
            }
            DataRow pushRow = pushIterator.next();
            DataRow otherRow = otherIterator.next();
            DataRow referenceRow = referenceIterator.next();
            assertEquals(referenceRow, pushRow);
            assertEquals(referenceRow, otherRow);
        }
        assertFalse(buffer.usesOutFile());
        assertFalse(tableIterator1.hasNext());
        assertFalse(tableIterator2.hasNext());

        Thread restoreThread = new Thread(new Runnable() {
            @Override
            public void run() {
                MemoryAlertSystem.getInstance().sendMemoryAlert();
            }
        }, "Buffer restore");

        tableIterator1 = container.getTable().iterator();
        referenceIterator = generateRows(count);
        for (i = 0; i < count; i++) {
            if (i == 10) {
                restoreThread.start();
            }
            DataRow row = tableIterator1.next();
            DataRow referenceRow = referenceIterator.next();
            assertEquals(referenceRow, row);
        }
        restoreThread.join();
        assertFalse(buffer.usesOutFile());
    }

    private static RowIterator generateRows(final int count) {
        return new RowIterator() {

            private int m_index = 0;

            @Override
            public DataRow next() {
                DefaultRow r = new DefaultRow(RowKey.createRowKey(m_index),
                        new StringCell("String " + m_index), new IntCell(m_index), new DoubleCell(m_index));
                m_index++;
                return r;
            }

            @Override
            public boolean hasNext() {
                return m_index < count;
            }
        };
    }

    /**
     * method being tested: getTable().
     */
    public final void testGetTable() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        try {
            c.getTable();
            fail("Expected " + IllegalArgumentException.class + " not thrown");
        } catch (IllegalStateException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }
        c.close();
        c.getTable();
    }

    /**
     * method being tested: addRowToTable().
     */
    public final void testDuplicateKey() {
        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        c.addRowToTable(r1);
        c.addRowToTable(r2);

        // add row 1 twice
        try {
            c.addRowToTable(r1);
            c.close();
            // ... eh eh, you don't do this
            fail("Expected " + DuplicateKeyException.class + " not thrown");
        } catch (DuplicateKeyException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }
    }

    /**
     * method being tested: addRowToTable().
     */
    public final void testIncompatibleTypes() {
        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        RowKey r3Key = new RowKey("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});
        c.addRowToTable(r1);
        c.addRowToTable(r2);
        c.addRowToTable(r3);

        // add incompatible types
        RowKey r4Key = new RowKey("row 4");
        DataCell r4Cell1 = new StringCell("Row 4, Cell 1");
        DataCell r4Cell2 = new DoubleCell(42.0); // not allowed
        DataRow r4 = new DefaultRow(r4Key, new DataCell[] {r4Cell1, r4Cell2});
        try {
            c.addRowToTable(r4);
            c.close();
            fail("Expected " + DataContainerException.class + " not thrown");
        } catch (DataContainerException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                throw e;
            } else {
                NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getCause().getClass(),
                                                       e.getCause());
            }
        } catch (IllegalArgumentException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }

    }

    /**
     * method being tested: addRowToTable().
     */
    public final void testWrongCellCountRow() {
        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        RowKey r3Key = new RowKey("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});
        c.addRowToTable(r1);
        c.addRowToTable(r2);
        c.addRowToTable(r3);

        // add wrong sized row
        RowKey r5Key = new RowKey("row 5");
        DataCell r5Cell1 = new StringCell("Row 5, Cell 1");
        DataCell r5Cell2 = new IntCell(52);
        DataCell r5Cell3 = new DoubleCell(53.0);
        DataRow r5 = new DefaultRow(
                r5Key, new DataCell[] {r5Cell1, r5Cell2, r5Cell3});
        try {
            c.addRowToTable(r5);
            c.close();
            fail("Expected " + DataContainerException.class + " not thrown");
        } catch (DataContainerException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                throw e;
            } else {
                NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getCause().getClass(),
                                                       e.getCause());
            }
        } catch (IllegalArgumentException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }
    }

    /**
     * method being tested: addRowToTable().
     */
    public final void testAddNullRow() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        c.addRowToTable(new DefaultRow(new RowKey("Row1"), new DataCell[0]));
        // add null
        try {
            c.addRowToTable((DataRow)null);
            c.close();
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }
    }

    /**
     * method being tested: addRowToTable().
     */
    public final void testRowOrder() {
        // addRow should preserve the order, we try here randomly generated
        // IntCells as key (the container puts it in a linked has map)
        DataCell[] values = new DataCell[0];
        Vector<RowKey> order = new Vector<RowKey>(500);
        for (int i = 0; i < 500; i++) {
            // fill it - this should be easy to preserve (as the int value
            // is also the hash code)
            order.add(new RowKey(Integer.toString(i)));
        }
        // shuffle it - that should screw it up
        Collections.shuffle(order);
        DataContainer c = new DataContainer(EMPTY_SPEC);
        for (RowKey key : order) {
            c.addRowToTable(new DefaultRow(key, values));
        }
        c.close();
        DataTable table = c.getTable();
        int pos = 0;
        for (RowIterator it = table.iterator(); it.hasNext(); pos++) {
            DataRow cur = it.next();
            assertEquals(cur.getKey().getString(), order.get(pos).getString());
        }
        assertEquals(pos, order.size());
    } // testAddRowToTable()

    /**
     * Try a big file :-).
     *
     */
    public void testBigFile() {
        // with these setting (50, 1000) it will write an 250MB cache file
        // (the latest data this value was checked: 31. August 2006...)
        final int colCount = 50;
        final int rowCount = 100;
        String[] names = new String[colCount];
        DataType[] types = new DataType[colCount];
        for (int c = 0; c < colCount; c++) {
            names[c] = "Column " + c;
            switch (c % 3) {
                case 0: types[c] = DoubleCell.TYPE; break;
                case 1: types[c] = StringCell.TYPE; break;
                case 2: types[c] = IntCell.TYPE; break;
                default: throw new InternalError();
            }
        }
        DataTableSpec spec = new DataTableSpec(names, types);
        names = null;
        types = null;
        DataContainer container = new DataContainer(spec);
        final ObjectToDataCellConverter conv = new ObjectToDataCellConverter();
        final long seed = System.currentTimeMillis();
        Random rand = new Random(seed);
        for (int i = 0; i < rowCount; i++) {
            DataRow row = createRandomRow(i, colCount, rand, conv);
            container.addRowToTable(row);
        }
        container.close();
        final Throwable[] throwables = new Throwable[1];
        final DataTable table = container.getTable();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    Random rand1 = new Random(seed);
                    for (RowIterator it = table.iterator();
                        it.hasNext(); i++) {
                        DataRow row1 =
                            createRandomRow(i, colCount, rand1, conv);
                        DataRow row2 = it.next();
                        assertEquals(row1, row2);
                    }
                    assertEquals(i, rowCount);
                } catch (Throwable t) {
                    throwables[0] = t;
                }
            }
        }; // Runnable
        // make two threads read the buffer (file) concurrently.
        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);
        t1.start();
        t2.start();
        try {
            // seems that the event dispatch thread must not release the
            // reference to the table, otherwise it is (I guess!!) garbage
            // collected: You comment these lines and see the error message.
            t1.join();
            t2.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            fail();
        }
        if (throwables[0] != null) {
            throw new RuntimeException(throwables[0]);
        }
    } // testBigFile()

    /** Restoring into main memory.
     * @see ContainerTable#restoreIntoMemory()*/
    public void testRestoreIntoMemory() {
        // with these setting (50, 100) it will write an 250MB cache file
        // (the latest data this value was checked: 31. August 2006...)
        final int colCount = 50;
        final int rowCount = 100;
        String[] names = new String[colCount];
        DataType[] types = new DataType[colCount];
        for (int c = 0; c < colCount; c++) {
            names[c] = "Column " + c;
            switch (c % 3) {
                case 0: types[c] = DoubleCell.TYPE; break;
                case 1: types[c] = StringCell.TYPE; break;
                case 2: types[c] = IntCell.TYPE; break;
                default: throw new InternalError();
            }
        }
        DataTableSpec spec = new DataTableSpec(names, types);
        names = null;
        types = null;
        DataContainer container = new DataContainer(spec, true, 0);
        final ObjectToDataCellConverter conv = new ObjectToDataCellConverter();
        final long seed = System.currentTimeMillis();
        Random rand = new Random(seed);
        for (int i = 0; i < rowCount; i++) {
            DataRow row = createRandomRow(i, colCount, rand, conv);
            container.addRowToTable(row);
            row = null;
        }
        container.close();
        assertTrue(container.getBufferedTable().getBuffer().usesOutFile());
        final Throwable[] throwables = new Throwable[1];
        final ContainerTable table = container.getBufferedTable();
        table.restoreIntoMemory();
        // different iterators restore the content, each of which one row
        RowIterator[] its = new RowIterator[10];
        for (int i = 0; i < its.length; i++) {
            its[i] = table.iterator();
            for (int count = 0; count < i + 1; count++) {
                its[i].next();
            }
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    Random rand1 = new Random(seed);
                    for (RowIterator it = table.iterator();
                        it.hasNext(); i++) {
                        DataRow row1 =
                            createRandomRow(i, colCount, rand1, conv);
                        DataRow row2 = it.next();
                        assertEquals(row1, row2);
                    }
                    assertEquals(i, rowCount);
                } catch (Throwable t) {
                    throwables[0] = t;
                }
            }
        }; // Runnable
        // make two threads read the buffer (file) concurrently.
        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);
        t1.start();
        t2.start();
        try {
            // seems that the event dispatch thread must not release the
            // reference to the table, otherwise it is (I guess!!) garbage
            // collected: You comment these lines and see the error message.
            t1.join();
            t2.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            fail();
        }
        if (throwables[0] != null) {
            throw new RuntimeException(throwables[0]);
        }
    } // testBigFile()

    /** Test if the domain is retained. */
    public void testTableDomain() {
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        RowKey r3Key = new RowKey("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});

        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        // add in different order
        c.addRowToTable(r2);
        c.addRowToTable(r1);
        c.addRowToTable(r3);
        c.close();
        DataTable table = c.getTable();
        DataTableSpec tableSpec = table.getDataTableSpec();

        // check possible values
        Set<DataCell> possibleValues =
            tableSpec.getColumnSpec(0).getDomain().getValues();
        assertEquals(possibleValues.size(), 3);
        assertTrue(possibleValues.contains(r1Cell1));
        assertTrue(possibleValues.contains(r2Cell1));
        assertTrue(possibleValues.contains(r3Cell1));
        // no possible values for integer column
        possibleValues = tableSpec.getColumnSpec(1).getDomain().getValues();
        assertNull(possibleValues);

        // check min max
        DataCell min = tableSpec.getColumnSpec(0).getDomain().getLowerBound();
        DataCell max = tableSpec.getColumnSpec(0).getDomain().getLowerBound();
        assertNull(min);
        assertNull(max);

        min = tableSpec.getColumnSpec(1).getDomain().getLowerBound();
        max = tableSpec.getColumnSpec(1).getDomain().getUpperBound();
        Comparator<DataCell> comparator =
            tableSpec.getColumnSpec(1).getType().getComparator();
        assertTrue(comparator.compare(min, max) < 0);
        assertEquals(min, r1Cell2);
        assertEquals(max, r3Cell2);
    }

    public void testAsyncWriteLimits() throws Exception {
        Assume.assumeTrue(!DataContainer.SYNCHRONOUS_IO);
        final int limit = Platform.ARCH_X86.equals(Platform.getOSArch()) ? 10 : 50;
        Assert.assertEquals(limit, DataContainer.MAX_ASYNC_WRITE_THREADS);
        RowIterator infinitIterator = generateRows(Integer.MAX_VALUE);
        List<DataContainer> containerList = new ArrayList<DataContainer>();
        try {
            boolean isAsync;
            do {
                int activeCount = DataContainer.ASYNC_EXECUTORS.getActiveCount();
                DataContainer c = new DataContainer(SPEC_STR_INT_DBL, true, 0);
                c.addRowToTable(infinitIterator.next());
                // no activeCount is incremented by one - so order of two lines is important.
                containerList.add(c);
                isAsync = activeCount <= limit;
                assertEquals("unexpected async write behavior, active thread count is " + activeCount,
                    isAsync, !c.isSynchronousWrite());
            } while (isAsync);
        } finally {
            for (DataContainer c : containerList) {
                c.close();
            }
        }
    }

    private static char[] createRandomChars(
            final int length, final Random rand) {
        char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (char)rand.nextInt(Character.MAX_VALUE);
        }
        return result;
    }

    private static DataRow createRandomRow(final int index, final int colCount,
            final Random rand1, final ObjectToDataCellConverter conv) {
        RowKey key = new RowKey("Row " + index);
        DataCell[] cells = new DataCell[colCount];
        for (int c = 0; c < colCount; c++) {
            DataCell cell = null;
            switch (c % 3) {
            case 0:
                cell = conv.createDataCell(
                        rand1.nextDouble() - 0.5);
                break;
            case 1:
                String s;
                if (rand1.nextDouble() < 0.1) {
                    s = new String(createRandomChars(
                            rand1.nextInt(1000000), rand1));
                } else {
                    s = "Row" + index + "; Column:" + c;
                }
                cell = conv.createDataCell(s);
                break;
            case 2:
                // use full range of int
                int r = (int)rand1.nextLong();
                cell = conv.createDataCell(r);
                break;
            default: throw new InternalError();
            }
            cells[c] = cell;
        }
        return new DefaultRow(key, cells);
    }

}
