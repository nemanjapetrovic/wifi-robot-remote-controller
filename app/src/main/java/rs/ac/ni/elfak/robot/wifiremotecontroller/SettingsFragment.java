package rs.ac.ni.elfak.robot.wifiremotecontroller;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class SettingsFragment extends Fragment {

    private EditText ip1, ip2, ip3, ip4,port;
    private ImageButton closeFragment;
    private Button save;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment,container,false);

        ip1 = (EditText) view.findViewById(R.id.ip1);
        ip2 = (EditText) view.findViewById(R.id.ip2);
        ip3 = (EditText) view.findViewById(R.id.ip3);
        ip4 = (EditText) view.findViewById(R.id.ip4);
        port = (EditText) view.findViewById(R.id.port);
        closeFragment = (ImageButton) view.findViewById(R.id.close_fragment);
        save = (Button) view.findViewById(R.id.savebutton);

        loadDataSharedPref();
        initClickListeners();
        return view;
    }

    private void initClickListeners() {
        closeFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getView().setVisibility(View.GONE);
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.ip), ip1.getText() + "." + ip2.getText() + "." + ip3.getText() + "." + ip4.getText());
                editor.putString(getString(R.string.port), String.valueOf(port.getText()));
                editor.commit();
            }
        });
    }

    private void loadDataSharedPref()
    {

    }

}
