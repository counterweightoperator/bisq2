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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BuyerState2 extends BaseState {
    private final Controller controller;

    public BuyerState2(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            model.getSendBtcAddressButtonDisabled().bind(model.getBtcAddress().isEmpty().and(model.getFiatPaymentConfirmed().not()));

            super.onActivate();
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            model.getSendBtcAddressButtonDisabled().unbind();
        }

        private void onConfirmFiatSent() {
            model.getFiatPaymentConfirmed().set(true);
        }

        private void onSendBtcAddress() {
            sendChatBotMessage(Res.get("bisqEasy.tradeState.info.buyer.phase2.chatBotMessage", model.getQuoteCode(), model.getBtcAddress().get()));
            try {
                bisqEasyTradeService.buyerConfirmFiatSent(model.getBisqEasyTrade(), model.getBtcAddress().get());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final StringProperty btcAddress = new SimpleStringProperty();
        private final BooleanProperty sendBtcAddressButtonDisabled = new SimpleBooleanProperty();
        private final BooleanProperty fiatPaymentConfirmed = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button confirmFiatSentButton, sendBtcAddressButton;
        private final MaterialTextField btcAddress;
        private final MaterialTextArea account;
        private final MaterialTextField quoteAmount;
        private final BisqText sendFiatHeadline, btcAddressHeadline;
        private Subscription fiatPaymentConfirmedPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            sendFiatHeadline = new BisqText();

            confirmFiatSentButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase2.confirmFiatSent", model.getBisqEasyOffer().getMarket().getQuoteCurrencyCode()));
            confirmFiatSentButton.setDefaultButton(true);

            sendBtcAddressButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase2.sendBtcAddress"));
            sendBtcAddressButton.setDefaultButton(true);

            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress"), "", true);
            btcAddress.setPromptText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.prompt"));

            account = FormUtils.addTextArea(Res.get("bisqEasy.tradeState.info.buyer.phase2.sellersAccount"),
                    "", false);

            btcAddressHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.headline", model.getFormattedBaseAmount()));

            VBox.setMargin(btcAddressHeadline, new Insets(10, 0, 0, 0));
            VBox.setMargin(sendBtcAddressButton, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    sendFiatHeadline,
                    quoteAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.quoteAmount"), "", false),
                    account,
                    confirmFiatSentButton,
                    btcAddressHeadline,
                    btcAddress,
                    sendBtcAddressButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            sendFiatHeadline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2.headline", model.getFormattedQuoteAmount()));
            quoteAmount.setText(model.getFormattedQuoteAmount());
            btcAddressHeadline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.headline", model.getFormattedQuoteAmount()));
            account.setText(model.getBisqEasyTrade().getPaymentAccountData().get());

            btcAddress.textProperty().bindBidirectional(model.getBtcAddress());
            sendBtcAddressButton.disableProperty().bind(model.getSendBtcAddressButtonDisabled());

            fiatPaymentConfirmedPin = EasyBind.subscribe(model.getFiatPaymentConfirmed(), fiatPaymentConfirmed -> {
                double dimmed = 0.3;
                if (fiatPaymentConfirmed) {
                    sendFiatHeadline.setOpacity(dimmed);
                    sendFiatHeadline.getStyleClass().remove("bisq-easy-trade-state-info-headline");
                    sendFiatHeadline.getStyleClass().add("bisq-easy-trade-state-info-text");

                    btcAddressHeadline.setOpacity(1);
                    btcAddressHeadline.getStyleClass().remove("bisq-easy-trade-state-info-text");
                    btcAddressHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");
                } else {
                    sendFiatHeadline.setOpacity(1);
                    sendFiatHeadline.getStyleClass().remove("bisq-easy-trade-state-info-text");
                    sendFiatHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

                    btcAddressHeadline.setOpacity(dimmed);
                    btcAddressHeadline.getStyleClass().remove("bisq-easy-trade-state-info-headline");
                    btcAddressHeadline.getStyleClass().add("bisq-easy-trade-state-info-text");
                }
                confirmFiatSentButton.setVisible(!fiatPaymentConfirmed);
                confirmFiatSentButton.setManaged(!fiatPaymentConfirmed);
                btcAddress.setDisable(!fiatPaymentConfirmed);
            });

            confirmFiatSentButton.setOnAction(e -> controller.onConfirmFiatSent());
            sendBtcAddressButton.setOnAction(e -> controller.onSendBtcAddress());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            btcAddress.textProperty().unbindBidirectional(model.getBtcAddress());
            sendBtcAddressButton.disableProperty().unbind();

            fiatPaymentConfirmedPin.unsubscribe();

            confirmFiatSentButton.setOnAction(null);
            sendBtcAddressButton.setOnAction(null);
        }
    }
}