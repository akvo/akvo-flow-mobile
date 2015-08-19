
package org.akvo.flow.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.akvo.flow.R;

public class OrderByDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {

    public interface OrderByDialogListener {
        public void onOrderByClick(int order);
    }
    
    OrderByDialogListener mListener;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            mListener = (OrderByDialogListener)getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement OrderByDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sort)
                .setItems(R.array.order_by, this);
        return builder.create();
    }

    public void onClick(DialogInterface dialog, int which) {
        mListener.onOrderByClick(which);
    }

}
