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

import bisq.desktop.components.controls.MultiLineLabel;
import bisq.desktop.main.content.academy.AcademyView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class BisqAcademyView extends AcademyView<BisqAcademyModel, BisqAcademyController> {

    protected MultiLineLabel exchangeDecentralizedHeadline;
    protected MultiLineLabel exchangeDecentralizedContent;
    protected MultiLineLabel whyBisqHeadline;
    protected MultiLineLabel whyBisqContent;

    protected MultiLineLabel tradeSafelyHeadline;
    protected MultiLineLabel tradeSafelyContent;

    public BisqAcademyView(BisqAcademyModel model, BisqAcademyController controller) {
        super(model, controller);

        String key = getKey();

        exchangeDecentralizedHeadline = new MultiLineLabel(Res.get("academy." + key + ".exchangeDecentralizedHeadline"));
        exchangeDecentralizedHeadline.getStyleClass().addAll("font-size-16", "font-light");
        exchangeDecentralizedHeadline.setWrapText(true);

        exchangeDecentralizedContent = new MultiLineLabel(Res.get("academy." + key + ".exchangeDecentralizedContent"));
        exchangeDecentralizedContent.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");
        exchangeDecentralizedContent.setWrapText(true);

        whyBisqHeadline = new MultiLineLabel(Res.get("academy." + key + ".whyBisqHeadline"));
        whyBisqHeadline.getStyleClass().addAll("font-size-16", "font-light");
        whyBisqHeadline.setWrapText(true);

        whyBisqContent = new MultiLineLabel(Res.get("academy." + key + ".whyBisqContent"));
        whyBisqContent.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");
        whyBisqContent.setWrapText(true);

        tradeSafelyHeadline = new MultiLineLabel(Res.get("academy." + key + ".tradeSafelyHeadline"));
        tradeSafelyHeadline.getStyleClass().addAll("font-size-16", "font-light");
        tradeSafelyHeadline.setWrapText(true);

        tradeSafelyContent = new MultiLineLabel(Res.get("academy." + key + ".tradeSafelyContent"));
        tradeSafelyContent.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");
        tradeSafelyContent.setWrapText(true);

        VBox.setMargin(exchangeDecentralizedHeadline, new Insets(25, 0, 0, 0));
        VBox.setMargin(whyBisqHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(tradeSafelyHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(tradeSafelyContent, new Insets(0, 0, 15, 0));
        root.getChildren().addAll(commonHeaderElementsCount - 1, Arrays.asList(
                exchangeDecentralizedHeadline,
                exchangeDecentralizedContent,
                whyBisqHeadline,
                whyBisqContent,
                tradeSafelyHeadline,
                tradeSafelyContent
        ));
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
