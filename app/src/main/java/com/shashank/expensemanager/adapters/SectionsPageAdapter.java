package com.shashank.expensemanager.adapters;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.shashank.expensemanager.fragments.BalanceFragment;
import com.shashank.expensemanager.fragments.ExpenseFragment;

import java.util.ArrayList;
import java.util.List;

public class SectionsPageAdapter extends FragmentPagerAdapter {

    private final List<Fragment> FragmentList=new ArrayList<>();
    private final List<String> FragmentTitleList=new ArrayList<>();

    public void addFragment(Fragment fragment,String title){
        FragmentList.add(fragment);
        FragmentTitleList.add(title);
    }

    public void refreshAllFragments() {
        try {
            for (Fragment fragment : FragmentList) {
                if (fragment != null && fragment.isAdded()) {
                    if (fragment instanceof ExpenseFragment) {
                        ((ExpenseFragment) fragment).refreshData();
                    } else if (fragment instanceof BalanceFragment) {
                        ((BalanceFragment) fragment).refreshData();
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SectionsPageAdapter", "Error refreshing fragments", e);
        }
    }

    public SectionsPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return FragmentTitleList.get(position);
    }

    @Override
    public Fragment getItem(int position) {
        return FragmentList.get(position);
    }

    @Override
    public int getCount() {
        return FragmentList.size();
    }
}
