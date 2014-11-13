package com.xchat.adapter;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MyFragmentPagerAdapter extends FragmentPagerAdapter{

	private List<Fragment>fragList;
	private List<String>titleList;
	public MyFragmentPagerAdapter(FragmentManager fm,List<Fragment> fragList,List<String>titleList) {
		super(fm);
		this.fragList=fragList;
		this.titleList=titleList;
	}

	@Override
	public Fragment getItem(int location) {
		return fragList.get(location);
	}

	@Override
	public int getCount() {
		return fragList.size();
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		return titleList.get(position);
	}
}
