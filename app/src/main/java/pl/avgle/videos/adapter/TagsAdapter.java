package pl.avgle.videos.adapter;

import android.view.View;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import pl.avgle.videos.R;
import pl.avgle.videos.bean.TagsBean;
import pl.avgle.videos.config.ImageConfig;

public class TagsAdapter extends BaseQuickAdapter<TagsBean.ResponseBean.CollectionsBean, BaseViewHolder> {

    public TagsAdapter(List list) {
        super(R.layout.tags_item, list);
    }

    @Override
    protected void convert(BaseViewHolder helper, TagsBean.ResponseBean.CollectionsBean item) {
        ImageView favoriteView = helper.getView(R.id.favorite_view);
        if (item.isFavorite())
            favoriteView.setVisibility(View.VISIBLE);
        else
            favoriteView.setVisibility(View.GONE);
        if (!item.getCover_url().equals("")) {
            ImageLoader.getInstance().displayImage(item.getCover_url(), (ImageView) helper.getView(R.id.collectionsImg), ImageConfig.getSimpleOptions());
            helper.setVisible(R.id.custom_tag, false);
        } else {
            helper.setVisible(R.id.custom_tag, true);
            helper.setImageResource(R.id.collectionsImg, R.drawable.default_image);
        }
        helper.setText(R.id.collectionsName, item.getTitle());
    }
}
