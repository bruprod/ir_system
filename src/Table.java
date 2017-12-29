import javax.swing.table.AbstractTableModel;

/**
 * Created by Andreas.
 */
public class Table extends AbstractTableModel {
    public Table(){
    }
    Object rows[][] = {{"Cat", Boolean.FALSE,}, {"Dog", Boolean.TRUE}};
    String[] headers = {"Search Results", "DocID"};

    /**
     * Table constructor for the table model
     * @param data containing the table data
     */
    public Table(Object[][] data){
        rows = data;
    }

    Object last_changed[][] ={{"", Boolean.FALSE}};

    /**
     * @return returns the amount of rows in the table
     */
    @Override
    public int getRowCount() {
        return rows.length;
    }

    /**
     * @return returns the amount of the columns of the table
     */
    @Override
    public int getColumnCount() {
        return rows[0].length;
    }

    /**
     * Returns the value at specific position
     * @param rowIndex the integer rowIndex of the table
     * @param columnIndex the integer columnIndex of the table
     * @return returns the data from the table at position rowIndex, columnIndex
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows[rowIndex][columnIndex];
    }

    /**
     * @param rowIndex the integer rowIndex of the table
     * @param columnIndex the integer columnIndex of the table
     * @return returns true or false if the cell can be edited
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex != 0);
    }

    /**
     * @param columnIndex the integer columnIndex of the table
     * @return returns the column class
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return rows[0][columnIndex].getClass();
    }

    /**
     * Sets the value at a specific position
     * @param aValue contains the value to be set
     * @param rowIndex the integer rowIndex of the table
     * @param columnIndex the integer columnIndex of the table
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        rows[rowIndex][columnIndex] = aValue;
        if(columnIndex == 1) {
            last_changed[0][columnIndex] = aValue;
            last_changed[0][0] = rows[rowIndex][0];
        }
        fireTableDataChanged();
    }

    /**
     * @param column the integer columnIndex of the table
     * @return returns the name of the columns from the column headers
     */
    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    /**
     * @return returns the object which has been changed recently
     */
    public Object[][] getLastChangedValue(){
        return last_changed;
    }


}
