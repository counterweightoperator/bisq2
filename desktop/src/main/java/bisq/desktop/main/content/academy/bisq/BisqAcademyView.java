/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.academy.bisq;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.desktop.main.content.academy.AcademyView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class BisqAcademyView extends AcademyView<BisqAcademyModel, BisqAcademyController> {

    protected MultiLineLabel overviewHeadline;
    protected MultiLineLabel overview;
    protected MultiLineLabel contentHeadline;
    protected MultiLineLabel content;

    public BisqAcademyView(BisqAcademyModel model, BisqAcademyController controller) {
        super(model, controller);

        String key = getKey();

        overviewHeadline = new MultiLineLabel(Res.get("academy.overview"));
        overviewHeadline.getStyleClass().addAll("font-size-16", "font-light");
        overviewHeadline.setWrapText(true);

        overview = new MultiLineLabel(Res.get("academy." + key + ".overview"));
        overview.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");
        overview.setWrapText(true);

        contentHeadline = new MultiLineLabel(Res.get("academy." + key + ".content.headline"));
        contentHeadline.getStyleClass().addAll("font-size-16", "font-light");
        contentHeadline.setWrapText(true);

        content = new MultiLineLabel(Res.get("academy." + key + ".content"));
        content.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");
        content.setWrapText(true);

        VBox.setMargin(overviewHeadline, new Insets(25, 0, 0, 0));
        VBox.setMargin(contentHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(content, new Insets(0, 0, 15, 0));
        root.getChildren().addAll(commonHeaderElementsCount - 1, Arrays.asList(overviewHeadline, overview, contentHeadline, content));
    }

    @Override
    protected String getKey() {
        return "bisq";
    }

    @Override
    protected String getIconId() {
        return "learn-bisq";
    }


    @Override
    protected String getUrl() {
        return "https://bisq.network/" + getKey();
    }
}
