package com.moe.fragment;
import android.os.*;
import android.support.v7.widget.*;
import android.view.*;
import android.view.animation.*;
import com.moe.entity.*;
import com.moe.view.*;
import com.moe.yaohuo.*;
import java.util.*;
import java.util.regex.*;
import org.jsoup.nodes.*;

import android.content.Intent;
import android.support.design.widget.AppBarLayout;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.Toast;
import com.moe.adapter.ListAdapter;
import com.moe.utils.PreferenceUtils;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
public class BbsListFragment extends AnimeFragment implements SwipeRefreshLayout.OnRefreshListener,ListAdapter.OnItemClickListener,AppBarLayout.OnOffsetChangedListener
{
	private ArrayList<ListItem> list;
	private ListAdapter la;
	private SwipeRefreshLayout refresh;
	private int page=1,total;
	private boolean isFirst;
	private boolean canLoadMore=true;
	private BbsItem bbs;
	private View progress;
	public void load(BbsItem bi)
	{
		bbs = bi;
		onRefresh();
	}
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null)
		{
			if (getArguments() != null)
			{
				bbs = getArguments().getParcelable("bbs");
			}
			else
			{
				bbs = new KeyItem();
				bbs.setAction("new");
				bbs.setTotal(2001);
				bbs.setClassid(0);
			}
		}
		else
		{
			bbs = savedInstanceState.getParcelable("bbs");
			list = savedInstanceState.getParcelableArrayList("list");
			page = savedInstanceState.getInt("page");
			canLoadMore = savedInstanceState.getBoolean("canloadmore");
			isFirst = savedInstanceState.getBoolean("isfirst");
			total = savedInstanceState.getInt("total");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (list == null)list = new ArrayList<>();
		AppBarLayout abl=(AppBarLayout) container.getRootView().findViewById(R.id.appbarlayout);
		//if(abl!=null)abl.addOnOffsetChangedListener(this);
		return inflater.inflate(R.layout.list_view, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		refresh = (SwipeRefreshLayout)view.findViewById(R.id.refresh);
		refresh.setOnRefreshListener(this);
		RecyclerView rv=(RecyclerView)refresh.getChildAt(1);
		rv.setLayoutManager(new GridLayoutManager(getActivity(), 1));
		rv.setAdapter(la = new ListAdapter(list));
		rv.addItemDecoration(new Divider(getResources().getDimensionPixelSize(R.dimen.cellSpacing)));
		ItemAnimation ia=new ItemAnimation(rv);
		ia.setAddDuration(290);
		ia.setRemoveDuration(150);
		rv.setItemAnimator(ia);
		rv.addOnScrollListener(new Scroll());
		la.setOnItemClickListener(this);
		progress = view.findViewById(R.id.progressbar);
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		if (list.size() == 0 && !(bbs.getAction().equals("search") && bbs.getKey() == null))
		{
			refresh.setRefreshing(true);

			onRefresh();
		}
		onHiddenChanged(isHidden());
		if (bbs.getAction().equals("new"))
			setHasOptionsMenu(true);
	}

	@Override
	public void onOffsetChanged(AppBarLayout p1, int p2)
	{
		refresh.setEnabled(p2 == 0);
	}


	@Override
	public Animation onCreateAnimation(int transit, boolean enter, int nextAnim)
	{
		if (enter)
		{
			AlphaAnimation aa=new AlphaAnimation(0, 1);
			aa.setDuration(300);
			return aa;
		}
		else
		{
			AlphaAnimation aa=new AlphaAnimation(1, 0);
			aa.setDuration(300);
			return aa;
		}

	}

	@Override
	public void onRefresh()
	{
		page = 1;
		isFirst = true;
		loadMore();
	}

	private void loadMore()
	{
		total = -1;
		if (!refresh.isRefreshing())
		{
			progress.setVisibility(View.VISIBLE);
		}
		new Thread(){
			public void run()
			{
				handler.obtainMessage(1, load()).sendToTarget();
			}
		}.start();
	}
	private List<ListItem> load()
	{
		Document doc=null;
		try
		{
			doc = Jsoup.connect(PreferenceUtils.getHost(getContext()) + getString(R.string.book_list))
				.data("action", bbs.getAction())
				.data("siteid", "1000")
				.data("classid", bbs.getClassid() + "")
				.data("key", bbs.getKey() == null ?"365": bbs.getKey())
				.data("getTotal", "")
				.data("type", bbs.getType() == null ?"days": bbs.getType())
				.data("page", "" + page)
				.userAgent("Mozilla/5.0 (Linux; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.132 Mobile Safari/537.36" + page)
				//.header("Cookie",moe.getString("cookie","))
				//.cookie("ASP.NET_SessionId","eblkqa45lstya2fqedir2h45")
				//.cookie("GUID","5e31fe031954")
				.cookie(PreferenceUtils.getCookieName(getContext()), PreferenceUtils.getCookie(getContext()))
				//.cookie("__cm_warden_uid","d0d4ece507182501dc13d8d0dcf2d7f2cookie")
				.header("Accept", "*/*").get();
		}
		catch (IOException e)
		{return null;}
		try
		{
			total = Integer.parseInt(doc.getElementsByAttributeValue("name", "getTotal").get(0).attr("value"));
		}
		catch (Exception e)
		{}
		Elements elements=doc.getElementsByAttributeValueMatching("class", "^line(1|2)$");
		List<ListItem> list=new ArrayList<>();
		for (int i=0;i < elements.size();i++)
		{
			try
			{
				ListItem li=new ListItem();
				Element element=elements.get(i);
				List<Node> childs=element.childNodes();
				li.setIndex(Integer.parseInt(childs.get(0).toString().trim().replace(".", "")));
				int n=1;
				List<String> ls=new ArrayList<>();
				for (;n < childs.size();n++)
				{
					Node node=childs.get(n);
					if (node.nodeName().equals("a"))
					{
						li.setProperty(ls);
						break;}
					else
					{
						ls.add(node.attr("alt"));
					}
				}
				Node node=childs.get(n);//地址
				String id=node.attr("href");
				li.setId(Integer.parseInt(id.substring(id.indexOf("-") + 1, id.indexOf("."))));
				Matcher matcher=Pattern.compile("<a.*?>(.*?)</a>").matcher(element.toString());
				if (matcher.find())
					li.setTitle(matcher.group(1));
				else
					li.setTitle(node.childNode(0).toString());
				n++;
				node = childs.get(++n);//用户名称
				if (node.nodeName().equals("img"))
				{
					node = childs.get(++n);
					if (node.toString().trim().length() == 0)
						node = childs.get(++n);
					li.setAuthor(node.toString().trim());
				}
				else
				{
					li.setAuthor(node.toString().trim());
				}
				if (li.getAuthor().endsWith("/"))
				{
					li.setAuthor(li.getAuthor().substring(0, li.getAuthor().length() - 1));
				}
				else
				{
					n++;
				}
				node = childs.get(++n);//回复数
				li.setProgress(node.childNode(0).toString());
				node = childs.get(++n);//回复总数
				String size=node.toString();
				li.setProgress("<font color='#0097a7'>" + li.getProgress() + "</font>/" + size.substring(size.indexOf("/") + 1, size.length() - 2));
				node = childs.get(++n);//时间
				li.setTime(node.childNode(0).toString());
				list.add(li);
			}
			catch (Exception e)
			{}
		}
		return list;
	}
	private Handler handler=new Handler(){

		@Override
		public void handleMessage(Message msg)
		{
			try
			{
				switch (msg.what)
				{
					case 0:
						refresh.setRefreshing(false);
						progress.setVisibility(progress.INVISIBLE);
						break;
					case 1:
						int size=0;
						refresh.setRefreshing(false);
						progress.setVisibility(progress.INVISIBLE);
						if (msg.obj == null)
						{
							Toast.makeText(getContext(), "访问失败", Toast.LENGTH_SHORT).show();
						}
						else
						{

							page++;
							if (isFirst)
							{
								isFirst = false;
								size = list.size();
								list.clear();
								la.notifyItemRangeRemoved(0, size);
								la.setAnime(false);
							}
							size = list.size();
							list.addAll((List)msg.obj);
							la.notifyItemRangeInserted(size, list.size() - size);
							canLoadMore = list.size() < total;

						}
						break;
				}
			}
			catch (Exception e)
			{}
		}

	};

	@Override
	public void onItemClick(RecyclerView.Adapter ra, RecyclerView.ViewHolder vh)
	{
		try
		{
			getActivity().startActivity(new Intent(getContext(), BbsActivity.class).putExtra("bbs", list.get(vh.getAdapterPosition())));
		}
		catch (Exception e)
		{}
	}


	private class Scroll extends RecyclerView.OnScrollListener
	{

		@Override
		public void onScrollStateChanged(RecyclerView recyclerView, int newState)
		{
			switch (newState)
			{
				case RecyclerView.SCROLL_STATE_IDLE:
					la.setAnime(false);
					break;
				case RecyclerView.SCROLL_STATE_SETTLING:
					la.setAnime(true);
					break;
			}

		}
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy)
		{
			if (dy > 0 && canLoadMore && !refresh.isRefreshing() && progress.getVisibility() != View.VISIBLE)
			{
				RecyclerView.LayoutManager ll=recyclerView.getLayoutManager();
				GridLayoutManager glm=(GridLayoutManager)ll;
				if (glm.findLastVisibleItemPosition() > glm.getItemCount() - glm.getSpanCount() * 4)loadMore();
			}
		}

	}

	@Override
	public void onHiddenChanged(boolean hidden)
	{
		if (bbs.getAction().equals("search"))return;
		View v=getView().getRootView().findViewById(R.id.edit);
		if (v != null)
		{
			if (hidden)
			{
				v.setVisibility(View.GONE);

			}
			else
			{
				v.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelable("bbs", bbs);
		outState.putParcelableArrayList("list", list);
		outState.putInt("page", page);
		outState.putBoolean("canloadmore", canLoadMore);
		outState.putInt("total", total);
		outState.putBoolean("isfirst", isFirst);

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.add(0, 0, 0, "搜索");
		menu.getItem(0).setIcon(VectorDrawableCompat.create(getResources(), R.drawable.magnify, getActivity().getTheme())).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case 0:
				getActivity().startActivity(new Intent(getContext(), SearchActivity.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

}