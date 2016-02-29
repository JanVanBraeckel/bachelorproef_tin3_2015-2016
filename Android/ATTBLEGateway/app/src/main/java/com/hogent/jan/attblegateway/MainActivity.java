package com.hogent.jan.attblegateway;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.hogent.jan.attblegateway.recyclerview.DeviceListAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements DeviceListFragment.DeviceListListener {
    private final String TAG = getClass().getSimpleName();

    @Bind(R.id.viewPager)
    ViewPager mViewPager;

    @Bind(R.id.tabLayout)
    TabLayout mPagerTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setupViewPager();
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        DeviceListFragment deviceListFragment = DeviceListFragment.newInstance();
        deviceListFragment.setDeviceClickedListener(this);
        adapter.addFragment(deviceListFragment, "Devices", "");
        mViewPager.setAdapter(adapter);

        mPagerTabs.setupWithViewPager(mViewPager);
    }

    /**
     * Adapter for the viewpager.
     */
    private static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();
        private final List<String> mDeviceAddresses = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title, String address) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
            mDeviceAddresses.add(address);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        public int hasTag(String deviceAddress){
            return mDeviceAddresses.indexOf(deviceAddress);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }


    @Override
    public void deviceClicked(BluetoothDevice device, int rssi) {
        Log.d(TAG, "deviceClicked() called with: " + "device = [" + device.getName() + "]");

        ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
        int index = adapter.hasTag(device.getAddress());

        if (index == -1) {
            DeviceDetailFragment deviceDetailFragment = DeviceDetailFragment.newInstance(device.getName(), device.getAddress(), rssi);
            adapter.addFragment(deviceDetailFragment, device.getName(), device.getAddress());
            adapter.notifyDataSetChanged();
            mPagerTabs.setTabsFromPagerAdapter(adapter);
            mViewPager.setCurrentItem(adapter.getCount(), true);
        }else{
            mViewPager.setCurrentItem(index+1, true);
        }
    }
}
