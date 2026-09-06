export default function UsePolicyPage() {
  return (
    <div className="max-w-4xl mx-auto my-3">
      <section className="border-t border-gray-200 pt-12">
        <h1 className="mb-6 text-3xl font-bold text-gray-950">
          Rolling Dice Sales Policy
        </h1>

        <h2 className="mb-3 text-2xl font-semibold">
          1. Single Card Purchases
        </h2>
        <ol className="list-decimal space-y-3 pl-6">
          <li>
            The value of single cards may change frequently. Therefore, the
            price is not finalized from the time a product is added to the cart
            until the order is completed. For credit card payments, the product
            price is finalized when the payment amount is displayed on the card
            company&apos;s payment screen. For direct payments, the product
            price is finalized when the order is completed.
          </li>
          <li>
            For this reason, once an order for single cards is completed,
            cancellation, exchange, or refund is not available unless the
            requirements under the exchange/refund policy are met. For direct
            payments, if payment is not confirmed by 4:00 PM on the day after
            the order date, Rolling Dice Co., Ltd. may cancel the order
            directly.
          </li>
          <li>
            Due to accidental issues or inventory management problems, the
            available stock may be insufficient to fulfill an order. In such
            cases, Rolling Dice Co., Ltd. will contact you directly and proceed
            with a refund or order cancellation. We kindly ask for your
            understanding.
          </li>
        </ol>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">
          2. TCG Sealed Products and Accessories
        </h2>
        <ol className="list-decimal space-y-3 pl-6">
          <li>
            We comply with each manufacturer&apos;s sales policies and maintain
            the standards expected of an online retailer by supplying products
            in the best possible undamaged condition.
          </li>
          <li>
            If you find damaged cards or products due to printing, logistics, or
            other issues while opening a sealed product, please contact Rolling
            Dice Co., Ltd. immediately. We will review the issue and do our best
            to resolve it. If the issue cannot be handled through Rolling Dice
            Co., Ltd., please contact the customer support center of the
            relevant manufacturer.
          </li>
          <li>
            Due to accidental issues or inventory management problems, the
            available stock may be insufficient to fulfill an order. In such
            cases, Rolling Dice Co., Ltd. will contact you directly and proceed
            with a refund or order cancellation. We kindly ask for your
            understanding.
          </li>
        </ol>
      </section>

      <section>
        <h1 className="mb-6 text-3xl font-bold text-gray-950">
          Rolling Dice Shipping Policy
        </h1>

        <h2 className="mb-3 text-2xl font-semibold">1. Shipping Fee</h2>
        <p>
          The standard shipping fee is KRW 3,500. Additional fees may apply for
          unusually large or bulky items, remote areas, islands, mountainous
          regions, or other locations where extra shipping costs are incurred.
        </p>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">
          2. Shipping Schedule
        </h2>
        <p className="mb-3">
          Orders are generally processed on the same day. However, depending on
          circumstances, orders may be processed as follows based on the day
          after the order date.
        </p>
        <ul className="list-disc space-y-2 pl-6">
          <li>
            Orders placed from Monday 12:00 AM to Friday 2:00 PM: packed and
            marked as ready for shipment at 3:00 PM on the order date, and
            handed over to the courier at 4:00 PM.
          </li>
          <li>
            Orders placed after Friday 2:00 PM until Monday 12:00 AM: packed and
            marked as ready for shipment at 3:00 PM on Monday, and handed over
            to the courier at 4:00 PM.
          </li>
          <li>
            For consumer damages caused by shipping delays, the applicable
            Consumer Damage Compensation Regulations shall apply.
          </li>
        </ul>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">3. Shipping Method</h2>
        <ol className="list-decimal space-y-2 pl-6">
          <li>Packaging: All products are packed in boxes with bubble wrap.</li>
          <li>Delivery: Orders are delivered by courier service.</li>
        </ol>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">
          4. Shipment Tracking
        </h2>
        <p>
          We currently use CJ Logistics. You can track your shipment using the
          CJ Logistics invoice tracking system. Please note that the tracking
          system is provided by the courier company and may not always be
          available.
        </p>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">
          5. Guest Purchases and Shipping
        </h2>
        <p>
          Guest purchases are available through the &quot;Guest Only&quot; link
          at the top of the website, where you can create a one-time account and
          use the service. Please note that reward points are not provided for
          guest purchases, and some services such as order confirmation may be
          limited.
        </p>
      </section>

      <section>
        <h1 className="mb-6 text-3xl font-bold text-gray-950">
          Rolling Dice Exchange and Refund Policy
        </h1>

        <h2 className="mb-3 text-2xl font-semibold">
          1. Exchange/Refund Requirements
        </h2>
        <p className="mb-4">
          Rolling Dice Co., Ltd. applies exchange and refund rules only when the
          following requirements are met according to product category. Exchange
          or refund requests must be made within 5 days of purchase.
        </p>

        <h3 className="mb-2 text-xl font-semibold">
          1&#41; TCG Sealed Products
        </h3>
        <ul className="mb-5 list-disc space-y-2 pl-6">
          <li>The delivered product is different from the order details.</li>
          <li>
            The order details are correct, and the protective packaging for
            courier delivery has not been opened.
          </li>
          <li>
            The order details are correct, and for display box purchases, the
            outer plastic wrapping has not been removed; or for individual
            booster pack purchases, the booster pack has not been opened.
          </li>
        </ul>

        <h3 className="mb-2 text-xl font-semibold">
          2&#41; Other Card Game Accessories
        </h3>
        <ul className="mb-5 list-disc space-y-2 pl-6">
          <li>The delivered product is different from the order details.</li>
          <li>
            The order details are correct, and the product&apos;s own packaging,
            excluding additional shipping packaging, has not been damaged.
          </li>
        </ul>

        <h3 className="mb-2 text-xl font-semibold">3&#41; Single Cards</h3>
        <ul className="mb-5 list-disc space-y-2 pl-6">
          <li>The delivered product is different from the order details.</li>
          <li>
            The order details are correct, but the opened product does not meet
            the condition grade standard presented by Rolling Dice Co., Ltd.
          </li>
        </ul>

        <div className="rounded-xl border border-gray-200 bg-gray-50 p-5">
          <h3 className="mb-4 text-xl font-semibold">
            Card Condition Grading Standard
          </h3>
          <dl className="space-y-4">
            <div>
              <dt className="font-bold">NM</dt>
              <dd>
                Generally considered the highest grade, referring to a card with
                almost no damage. Although the edges may not be perfectly clean
                due to production issues, there are almost no white marks, the
                card surface has almost no marks, and the card appears undamaged
                at a glance.
              </dd>
            </div>
            <div>
              <dt className="font-bold">EX</dt>
              <dd>
                A card with slight signs of use and some bending around the
                corners, but with no issue for gameplay. The edges may show
                slight wear, and minor marks may be visible at a glance.
              </dd>
            </div>
            <div>
              <dt className="font-bold">VG</dt>
              <dd>
                A card with heavy signs of use but no issue for gameplay. Many
                white marks may be visible on the edges, and scratches or damage
                can be found even with a rough inspection.
              </dd>
            </div>
            <div>
              <dt className="font-bold">G</dt>
              <dd>
                A card with no issue for gameplay, but with severe wear, dirty
                edges, and many white marks. If used without a card sleeve, the
                card may be clearly identifiable from the back.
              </dd>
            </div>
            <div>
              <dt className="font-bold">Below Grade</dt>
              <dd>
                A card with extreme damage, such as tearing or separation of
                card layers, making it distinguishable from other cards even
                when a sleeve is used. Such cards are not handled by us.
              </dd>
            </div>
          </dl>
        </div>

        <p className="mt-4 text-sm text-gray-600">
          ※ Since single card prices may change frequently, exchanges or refunds
          due to a change of mind caused by price changes are not available. ※
          Single cards damaged due to the buyer&apos;s negligence or intent are
          not eligible for exchange or refund.
        </p>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">
          2. Exchange/Refund Procedure
        </h2>
        <ol className="list-decimal space-y-3 pl-6">
          <li>
            Request an exchange or refund through the contact
            information&#40;phone number or email&#41; at the bottom of the
            shopping mall main page, or through the KakaoTalk 1:1 consultation
            button at the top of the logged-in screen.
          </li>
          <li>
            Once the request is received, visit the address shown at the bottom
            of the shopping mall main page with the product, or send the product
            by courier. &#40;Please confirm whether shipping fees are payable
            when making the exchange/refund request.&#41;
          </li>
          <li>
            Rolling Dice Co., Ltd. will inspect the product condition, decide
            whether exchange or refund is available, and contact you again. If
            exchange or refund is not available, the product will be returned to
            the buyer by collect-on-delivery shipping. If exchange or refund is
            approved, the refund will be processed through the original payment
            method, or the product will be exchanged and shipped again.
          </li>
        </ol>
      </section>

      <hr className="my-10 border-slate-200" />

      <h1 className="mb-6 text-3xl font-bold text-gray-950">
        Demo TCG Shop 판매 관련 정책
      </h1>
      <section>
        <h2 className="mb-3 text-2xl font-semibold">1. 낱장 카드 구매</h2>
        <ol className="list-decimal space-y-3 pl-6">
          <li>
            낱장 카드의 경우 카드 가치의 변화가 수시로 일어납니다. 그로 인해
            장바구니에 담고 이를 주문완료 할때까지 가격이 확정되지 않습니다.
            카드결제시에는 카드사 결제화면의 금액이 나올때 제품 가격이 결정되며,
            직접결제시에는 주문완료 될때 제품 가격이 결정됩니다.
          </li>
          <li>
            이러한 이유로 낱장 카드는 주문완료하면 교환/환불 정책에 따른 요건을
            충족하지 않는 한 주문취소, 제품 교환/환불이 불가합니다. 또한
            직접결제의 경우, 주문완료하신 날의 다음날 오후 4시까지 결제가
            확인되지 않는 경우, 주문이 Demo TCG Shop에 의해 직접 취소됩니다.
          </li>
          <li>
            재고 수량과 관련하여 우연 및 낱장 카드 재고 관리 등의 문제로 인해
            보유 수량이 부족하여 주문 사항을 지킬 수 없는 경우가 발생할 수
            있습니다. 이 경우 Demo TCG Shop가 직접 연락드리고 환불 및 주문
            취소를 진행해드립니다. 넓은 아량과 이해를 부탁드립니다.
          </li>
        </ol>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">
          2. TCG 밀봉 제품 및 기타 악세사리 제품 구매
        </h2>
        <ol className="list-decimal space-y-3 pl-6">
          <li>
            TCG 제품은 각 제조사의 판매 규정과 인터넷 리테일러로서의 자세를
            준수하며 손상 없는 최상의 제품을 공급합니다.
          </li>
          <li>
            밀봉 제품을 수령하시고 개봉하시는 과정에 인쇄, 물류 등의 문제로
            파손된 카드 혹은 제품을 확인하시는 경우, 즉시 Demo TCG Shop에
            연락하시면 확인 후 문제 해결을 위해 최선을 다하겠습니다. 만약
            Demo TCG Shop를 통한 처리가 어려운 경우 제품별 제조사 고객센터를
            통하여주시기 바랍니다.
          </li>
          <li>
            재고 수량과 관련하여 우연 및 제품 재고 관리 등의 문제로 인해 보유
            수량이 부족하여 주문 사항을 지킬 수 없는 경우가 발생할 수 있습니다.
            이 경우 Demo TCG Shop가 직접 연락드리고 환불 및 주문 취소를
            진행해드립니다. 넓은 아량과 이해를 부탁드립니다.
          </li>
        </ol>
      </section>

      <section>
        <h1 className="mb-6 text-3xl font-bold text-gray-950">
          Demo TCG Shop 배송 정책
        </h1>

        <h2 className="mb-3 text-2xl font-semibold">1. 배송비용</h2>
        <p>
          배송비는 기본 3,500원 입니다. 예외적으로 규모나 크기가 크거나,
          도서산간 등 추가 비용이 발생할 지역에 대해서는 추가 비용이 청구될 수
          있습니다.
        </p>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">2. 배송기간</h2>
        <p className="mb-3">
          원칙적으로 당일 처리이나, 경우에 따라 주문일 다음날 기준으로 아래와
          같이 처리될 수 있습니다.
        </p>
        <ul className="list-disc space-y-2 pl-6">
          <li>
            월요일 0시부터 금요일 오후 2시까지 주문 : 주문 당일 오후 3시에 포장
            및 출고 완료 처리되며 오후 4시에 택배 접수합니다.
          </li>
          <li>
            금요일 오후 2시 이후 월요일 0시까지 주문 : 월요일 오후 3시에 포장 및
            출고 완료 처리되며 오후 4시에 택배 접수합니다.
          </li>
          <li>
            배송지연 등에 따른 소비자 피해에 대해서는 소비자 피해
            보상규정&#40;재정경제부 고시&#41;을 적용합니다.
          </li>
        </ul>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">3. 배송방식</h2>
        <ol className="list-decimal space-y-2 pl-6">
          <li>포장 : 전 제품 에어캡 사용 및 박스 포장합니다.</li>
          <li>배송 : 택배를 사용합니다.</li>
        </ol>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">4. 배송조회</h2>
        <p>
          현재 CJ대한통운과 계약되어 있으며, CJ대한통운 송장번호 조회시스템으로
          배송상황을 조회할 수 있습니다. 조회시스템은 택배사가 제공하는
          서비스이므로 경우에 따라 조회가 불가능할 수 있음을 양해바랍니다.
        </p>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">
          5. 비회원 구매 및 배송
        </h2>
        <p>
          비회원 구매는 홈페이지 화면 상단의 &quot;비회원 전용&quot; 링크를
          이용하여 일회용 계정을 생성하여 접속하여 사용하실 수 있습니다. 비회원
          구매시에는 적립금을 제공하지 않으며 주문확인 등의 서비스 사용에
          어려움이 있을 수 있음을 양해바랍니다.
        </p>
      </section>

      <section>
        <h1 className="mb-6 text-3xl font-bold text-gray-950">
          Demo TCG Shop 교환 및 환불 정책
        </h1>

        <h2 className="mb-3 text-2xl font-semibold">1. 교환/환불 요건</h2>
        <p className="mb-4">
          Demo TCG Shop에서는 제품 분류에 따라 다음 요건을 만족하는 경우에 한해
          교환 및 환불 규정을 적용합니다. 단, 구매 후 5일 이내에 교환/환불
          요청을 해주셔야 합니다.
        </p>

        <h3 className="mb-2 text-xl font-semibold">1&#41; TCG 밀봉 제품</h3>
        <ul className="mb-5 list-disc space-y-2 pl-6">
          <li>주문 내용과 다른 제품이 배송된 경우</li>
          <li>
            주문 내용에는 문제가 없으며 택배 배송을 위한 보호 포장을 개봉하지
            않은 경우
          </li>
          <li>
            주문 내용에는 문제가 없으며 박스&#40;DP&#41; 구매시에 박스 겉비닐
            포장을 벗기지 않은 경우 혹은 낱개 부스터팩 제품 구매시에 부스터팩을
            개봉하지 않은 경우
          </li>
        </ul>

        <h3 className="mb-2 text-xl font-semibold">
          2&#41; 기타 카드게임 악세사리류
        </h3>
        <ul className="mb-5 list-disc space-y-2 pl-6">
          <li>주문 내용과 다른 제품이 배송된 경우</li>
          <li>
            주문 내용에는 문제가 없으며 배송을 위한 추가 포장을 제외한 제품
            자체의 포장을 파손하지 않은 경우
          </li>
        </ul>

        <h3 className="mb-2 text-xl font-semibold">3&#41; 낱장 카드</h3>
        <ul className="mb-5 list-disc space-y-2 pl-6">
          <li>주문 내용과 다른 제품이 배송된 경우</li>
          <li>
            주문 내용에는 문제가 없으며 개봉한 제품이 Demo TCG Shop가 제시한
            상태 등급 기준에 맞지 않은 경우
          </li>
        </ul>

        <div className="rounded-xl border border-gray-200 bg-gray-50 p-5">
          <h3 className="mb-4 text-xl font-semibold">판매 등급 산정 기준</h3>
          <dl className="space-y-4">
            <div>
              <dt className="font-bold">NM</dt>
              <dd>
                일반적으로 최상급으로 이야기되며, 거의 손상이 없는 상태를
                말합니다. 생산상의 문제로 테두리가 말끔하지는 않으나 하얀 자국이
                거의 없으며, 카드 표면은 거의 자국이 없으며 얼핏 봐서는 아무런
                손상이 없어 보이는 상태를 말합니다.
              </dd>
            </div>
            <div>
              <dt className="font-bold">EX</dt>
              <dd>
                약간 사용감이 있고, 모서리 쪽으로 휘어져 있는 카드이나
                게임용으로는 문제가 없는 상태를 말합니다. 테두리가 약간 달아
                있을 수 있으며, 얼핏 보았을때 약간의 상처가 보이는 경우를
                말합니다.
              </dd>
            </div>
            <div>
              <dt className="font-bold">VG</dt>
              <dd>
                게임하는데는 문제가 없으며 많은 사용감이 있는 카드이며, 테두리에
                하얀색 손상이 많이 보이는 카드를 말합니다. 대강 보아도 상처들을
                발견할 수 있는 경우입니다.
              </dd>
            </div>
            <div>
              <dt className="font-bold">G</dt>
              <dd>
                게임하는데는 문제가 없으나 마모가 심하고 테두리가 지저분하며
                하얀 자국이 많이 보이며, 카드 슬리브에 넣지 않고 사용한다면
                명확하게 어떤 카드인지 뒷면으로 구분이 가능한 상태를 말합니다.
              </dd>
            </div>
            <div>
              <dt className="font-bold">등급외</dt>
              <dd>
                찢어져 있거나, 카드의 층이 분리되어 앞뒤로 분해되는 등의 상태가
                극심하여 슬리브를 사용하더라도 다른 카드와 구분이 가능한 카드를
                말합니다. 이는 취급하지 않습니다.
              </dd>
            </div>
          </dl>
        </div>

        <p className="mt-4 text-sm text-gray-600">
          ※ 낱장 카드 제품은 수시로 가격이 변화하므로 가격 변화에 따른 변심의
          경우 교환/환불이 불가합니다. ※ 구매자의 과실 혹은 고의로 파손한 낱장
          카드는 교환/환불이 불가합니다.
        </p>

        <h2 className="mb-3 mt-8 text-2xl font-semibold">2. 교환/환불 절차</h2>
        <ol className="list-decimal space-y-3 pl-6">
          <li>
            쇼핑몰 메인 화면 하단의 Demo TCG Shop 연락처&#40;전화번호,
            이메일&#41; 또는 로그인한 화면 상단의 카카오톡 1:1 상담 버튼을 통해
            교환/환불 요청합니다.
          </li>
          <li>
            요청이 접수되면 쇼핑몰 메인 화면 하단의 주소로 제품을 가지고
            방문하시거나 택배를 통해 배송합니다. &#40;배송시 배송비는 교환/환불
            요청시 지불 여부를 확인하십시오&#41;
          </li>
          <li>
            Demo TCG Shop가 상품의 상태를 확인하고, 교환/환불 여부를 결정 후
            다시 연락드립니다. 교환/환불이 불가하면 다시 해당 제품을 구매자에게
            착불로 배송합니다. 교환/환불을 하기로 결정하면 구매자가 수행하신
            결제 수단을 통해 환불하거나 다른 제품으로 교환 후 재배송이
            진행됩니다.
          </li>
        </ol>
      </section>
    </div>
  );
}
