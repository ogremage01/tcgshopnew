"use client";

import { Button } from "@/components/ui/button";
import {
  useAdminProductSync,
  useAdminProductPriceConfig,
  useAdminProductStorage,
} from "@/app/admin/_hooks/productHooks";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardDescription,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { formatDateTime } from "@/utils/date";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import ProductUploadFileSelect from "@/app/admin/products/config/_components/ProductUploadFileSelect";
import StorageAddModal from "@/app/admin/products/config/_components/StorageAddModal";
import StorageEditModal from "@/app/admin/products/config/_components/StorageEditModal";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useCardProductBulkUpload } from "@/app/admin/products/config/_hooks/useCardProductBulkUpload";
import type { ProductConfigBootstrap } from "@/app/admin/products/config/_hooks/useProductConfigBootstrap";
import type { FormEvent } from "react";
import { apiClient } from "@/lib/api.client";
import { toast } from "sonner";

import { fromGameName } from "@/config/gameEnum";

type Props = { data: ProductConfigBootstrap };

function gameAbbrFromFullName(fullName: string): string {
  return fromGameName(fullName)?.gameAbbr ?? fullName.toLowerCase();
}

export default function ProductConfigPageContent({ data }: Props) {
  const {
    handleMetadataSync,
    handleSinglePriceSync,
    syncGameList,
    lastPriceSyncTime,
    lastMetadataSyncTime,
    lastOpenBinderSyncTime,
    lastImageDownloadSyncTime,
    lastPriceLinkOverwriteSyncTime,
    loadingSyncGames,
    loadingLastSync,
    handleTcgPImagesDownload,
    handleOpenBinderImagesDownload,
    handleScryfallImagesDownload,
    handleOpenBinderPricesSync,
    handleCheckCodeRebuild,
    handleCheckCodeRebuildSelective,
    handleLinkRebuild,
    handleFullRebuild,
    handlePriceLinkRescanNulls,
    handleSaveAllPricesToUnion,
    handleUnionPublicIdBackfill,
    handleStockCharge,
  } = useAdminProductSync({
    initialData: {
      games: data.sync.games,
      lastTime: data.sync.lastTime,
    },
  });

  const {
    handleExchangeRateChange,
    submittingGame,
    loadingMinimumPrices,
    loadingGradePrice,
    loadingCurrencyRates,
    minimumPrices,
    currencyRates,
    handleMinimumPriceChange,
    handleGradePriceChange,
    gradePrice,
  } = useAdminProductPriceConfig({
    initialData: {
      minimumPrices: data.price.minimumPrices,
      grade: data.price.grade,
      currencyRates: data.price.currencyRates,
    },
  });

  const {
    loadingStorageList,
    storageList,
    handleStorageAdd,
    storageAddModalOpen,
    setStorageAddModalOpen,
    storageEditModalOpen,
    setStorageEditModalOpen,
    handleStorageEdit,
  } = useAdminProductStorage({ initialList: data.storage });

  const { uploadResult, uploadedFileName, handleUploadCardProduct } =
    useCardProductBulkUpload();

  const handleGradePriceChangeWithConfirm = (e: FormEvent<HTMLFormElement>) => {
    const isConfirmed = window.confirm(
      "등급 가격비율을 변경하면 상품 가격이 재계산됩니다. 계속 진행하시겠습니까?",
    );

    if (!isConfirmed) {
      e.preventDefault();
      return;
    }

    handleGradePriceChange(e);
  };

  const handleRebuildProductSearchMap = () => {
    apiClient
      .post(
        "/api/admin/product/single-products/sync/product-search-map-reference-axis",
      )
      .then(() =>
        toast.info(
          "ProductSearchMap 참조축 리빌드가 시작되었습니다. 완료까지 시간이 걸릴 수 있습니다.",
        ),
      )
      .catch((err) => {
        if (err?.response?.status === 409) {
          toast.warning("이미 리빌드가 진행 중입니다.");
        } else {
          toast.error("리빌드 요청 중 오류가 발생했습니다.");
        }
      });
  };

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">싱글카드 설정</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card>
          <CardHeader>
            <CardTitle>싱글카드 일괄 등록</CardTitle>
            <CardDescription>
              xlsx파일 전용. <br />한 줄이라도 에러 발생 시 업로드가 되지
              않습니다.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="flex gap-2" onSubmit={handleUploadCardProduct}>
              <Input type="file" name="cardFile" accept=".xlsx" />
              <Button className="w-fit" type="submit">
                등록
              </Button>
            </form>
            <Separator className="my-4" />
            <Card>
              <CardHeader>
                <CardTitle>결과 안내</CardTitle>
                <CardDescription>
                  업로드 파일명: {uploadedFileName}
                </CardDescription>
                <Separator />
              </CardHeader>
              <CardContent>
                <ScrollArea className="h-[200px]">
                  {uploadResult ? (
                    <div className="flex flex-col gap-2">
                      <div className="flex flex-row gap-2 items-center justify-start">
                        <span className="font-bold text-green-500">
                          성공: {uploadResult.successCount}
                        </span>
                        <Separator className="h-4" orientation="vertical" />
                          <span className="font-bold text-red-500">
                            실패: {uploadResult.failCount}
                          </span>
                        <Separator className="h-4" orientation="vertical" />
                          <span className="font-bold text-blue-500">
                            재고 추가: {uploadResult.updatedCount}
                          </span>
                      </div>
                      <Separator />
                      {uploadResult.errors &&
                        uploadResult.errors.map((error) => (
                          <div key={error.rowNumber}>
                            <p>행 번호: {error.rowNumber}</p>
                            <p>행 데이터: {JSON.stringify(error.rowData)}</p>
                            <p>
                              오류 필드:{" "}
                              {error.fieldErrors
                                .map((fieldError) => fieldError.field)
                                .join(", ")}
                            </p>
                          </div>
                        ))}
                    </div>
                  ) : (
                    <p>등록 전입니다.</p>
                  )}
                </ScrollArea>
              </CardContent>
            </Card>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>일괄 등록용 파일 다운로드</CardTitle>
            <CardDescription>
              일괄 등록용 파일을 다운로드합니다.
            </CardDescription>
            <Separator />
          </CardHeader>
          <CardContent>
            {loadingStorageList ? (
              <div className="flex flex-col gap-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-9 w-24" />
              </div>
            ) : (
              <ProductUploadFileSelect storages={storageList} />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>TCGPlayer 동기화 목록</CardTitle>
            <Separator />
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>게임 이름</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loadingSyncGames
                  ? [1, 2, 3, 4, 5].map((i) => (
                      <TableRow key={i}>
                        <TableCell>
                          <Skeleton className="h-5 w-10" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-5 w-40" />
                        </TableCell>
                      </TableRow>
                    ))
                  : syncGameList.map((game) => (
                      <TableRow key={game.id}>
                        <TableCell>{game.id}</TableCell>
                        <TableCell>{game.productLineName}</TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
      <Card className="my-4">
        <CardHeader>
          <CardTitle>
            저장소 목록
            <StorageAddModal
              handleStorageAdd={handleStorageAdd}
              open={storageAddModalOpen}
              onOpenChange={setStorageAddModalOpen}
            />
          </CardTitle>
          <CardDescription>
            저장소 목록을 관리합니다. 삭제는 지원하지 않습니다.
          </CardDescription>
          <Separator />
        </CardHeader>
        <CardContent>
          <ScrollArea className="h-[500px]">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>저장소 이름</TableHead>
                  <TableHead>저장소 설명</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loadingStorageList
                  ? [1, 2, 3, 4].map((i) => (
                      <TableRow key={i}>
                        <TableCell>
                          <Skeleton className="h-5 w-8" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-5 w-32" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-5 w-48" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-8 w-16" />
                        </TableCell>
                      </TableRow>
                    ))
                  : storageList.map((storage) => (
                      <TableRow key={storage.id}>
                        <TableCell>{storage.id}</TableCell>
                        <TableCell>
                          {storage.storageName}{" "}
                          {storage.isDefault ? <Badge>기본</Badge> : ""}
                        </TableCell>
                        <TableCell>{storage.description}</TableCell>
                        <TableCell>
                          <StorageEditModal
                            handleStorageEdit={handleStorageEdit}
                            storage={storage}
                            open={storageEditModalOpen}
                            onOpenChange={setStorageEditModalOpen}
                          />
                        </TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>
          </ScrollArea>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-3 gap-4 my-4">
        <Card>
          <CardHeader>
            <CardTitle>등급 별 카드 가격 정책</CardTitle>
            <CardDescription>
              등급 별 카드 가격 정책을 관리합니다. <br /> 가격비율은 0.00~1.00
              사이의 값을 입력해주세요.
            </CardDescription>
            <Separator />
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-1/3">등급</TableHead>
                  <TableHead className="w-2/3">가격비율(0.00~1.00)</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loadingGradePrice
                  ? [1, 2, 3].map((i) => (
                      <TableRow key={i}>
                        <TableCell>
                          <Skeleton className="h-8 w-16" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-8 w-full max-w-xs" />
                        </TableCell>
                      </TableRow>
                    ))
                  : gradePrice.map((price) => (
                      <TableRow key={price.id}>
                        <TableCell>{price.grade}</TableCell>
                        <TableCell>
                          <form
                            className="flex gap-2"
                            onSubmit={handleGradePriceChangeWithConfirm}
                          >
                            <Input
                              name="gradeId"
                              value={price.id}
                              type="hidden"
                            />
                            <Input
                              name="grade"
                              value={price.grade}
                              type="hidden"
                            />
                            <Input
                              type="number"
                              step="0.01"
                              min="0.1"
                              max="1.00"
                              placeholder="0.00~1.00"
                              name="percentage"
                              defaultValue={price.percentage}
                            />
                            <Button className="w-fit" type="submit">
                              변경
                            </Button>
                          </form>
                        </TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
        <div className="flex flex-col gap-2">
          <Card>
            <CardHeader className="flex flex-col gap-2">
              <div className="flex flex-row gap-2 items-center justify-between">
                <CardTitle>TCGPlayer 동기화</CardTitle>
                <CardDescription>(설정 시간:매일 02시)</CardDescription>
              </div>
              <Separator />
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              {loadingLastSync ? (
                <>
                  <div className="flex flex-row gap-2 items-end justify-between">
                    <Skeleton className="h-12 w-2/3 max-w-md" />
                    <Skeleton className="h-9 w-32 shrink-0" />
                  </div>
                  <Separator />
                  <div className="flex flex-row gap-2 items-end justify-between">
                    <Skeleton className="h-12 w-2/3 max-w-md" />
                    <Skeleton className="h-9 w-36 shrink-0" />
                  </div>
                </>
              ) : (
                <>
                  <div className="flex flex-row gap-2 items-end justify-between">
                    <CardDescription>
                      최종 동기일:{" "}
                      {formatDateTime(lastMetadataSyncTime?.startTime)}
                      <span
                        className={`font-bold ${
                          lastMetadataSyncTime?.result === "success"
                            ? "text-green-500"
                            : "text-red-500"
                        }`}
                      >
                        {" "}
                        {lastMetadataSyncTime?.result === "success"
                          ? "성공"
                          : "실패"}
                      </span>
                    </CardDescription>
                    <Button className="w-fit" onClick={handleMetadataSync}>
                      메타데이터 동기화
                    </Button>
                  </div>
                  <div className="flex flex-row gap-2 items-end justify-between">
                    <CardDescription>
                      최종 동기일:{" "}
                      {formatDateTime(lastPriceSyncTime?.startTime)}
                      <span
                        className={`font-bold ${
                          lastPriceSyncTime?.result === "success"
                            ? "text-green-500"
                            : "text-red-500"
                        }`}
                      >
                        {" "}
                        {lastPriceSyncTime?.result === "success"
                          ? "성공"
                          : "실패"}
                      </span>
                    </CardDescription>
                    <Button className="w-fit" onClick={handleSinglePriceSync}>
                      싱글카드가격 동기화
                    </Button>
                  </div>
                  <Separator />
                  <div className="flex flex-row gap-2 items-end justify-between">
                    <CardDescription>
                      최종 동기일:{" "}
                      {formatDateTime(lastImageDownloadSyncTime?.startTime)}
                      <span
                        className={`font-bold ${
                          lastImageDownloadSyncTime?.result === "success"
                            ? "text-green-500"
                            : "text-red-500"
                        }`}
                      >
                        {" "}
                        {lastImageDownloadSyncTime?.result === "success"
                          ? "성공"
                          : "실패"}
                      </span>
                    </CardDescription>
                    <Button
                      className="w-fit"
                      onClick={handleTcgPImagesDownload}
                    >
                      이미지 다운로드
                    </Button>
                  </div>
                </>
              )}
              <Separator />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <div className="flex flex-row gap-2 items-center justify-between">
                <CardTitle> MTG/FAB 동기화</CardTitle>
                <CardDescription>(설정 시간:매일 02시, 14시 하루 2회)</CardDescription>
              </div>
              <Separator />
            </CardHeader>
            <CardContent>
              <div className="flex flex-row gap-2 items-end justify-between">
                <CardDescription>
                  최종 동기일:{" "}
                  {formatDateTime(lastOpenBinderSyncTime?.startTime)}
                  <span
                    className={`font-bold ${
                      lastOpenBinderSyncTime?.result === "success"
                        ? "text-green-500"
                        : "text-red-500"
                    }`}
                  >
                    {" "}
                    {lastOpenBinderSyncTime?.result === "success"
                      ? "성공"
                      : "실패"}
                  </span>
                </CardDescription>

                <Button className="w-fit" onClick={handleOpenBinderPricesSync}>
                  가격 동기화 시작
                </Button>
              </div>
              <div className="mt-3 flex flex-row gap-2 items-end justify-between">
                <CardDescription>
                  외부 소스의 MTG/FAB 카드 이미지를 내부 /card-images 경로로 다운로드합니다.
                </CardDescription>
                <Button
                  className="w-fit"
                  variant="outline"
                  onClick={handleOpenBinderImagesDownload}
                >
                  외부 카드 이미지 다운로드
                </Button>
              </div>
              <div className="mt-3 flex flex-row gap-2 items-end justify-between">
                <CardDescription>
                  외부 이미지 다운로드가 끝난 뒤 자동으로 이어서 실행됩니다. 실패한 MTG만 Scryfall에서 jpg로 받습니다.
                </CardDescription>
                <Button
                  className="w-fit"
                  variant="outline"
                  onClick={handleScryfallImagesDownload}
                >
                  Scryfall 이미지 다운로드
                </Button>
              </div>
              <Separator />
              <div className="flex flex-col gap-2">
                <CardDescription>
                  최종 동기일:{" "}
                  {formatDateTime(lastPriceLinkOverwriteSyncTime?.startTime)}
                  <span
                    className={`font-bold ${
                      lastPriceLinkOverwriteSyncTime?.result === "success"
                        ? "text-green-500"
                        : "text-red-500"
                    }`}
                  >
                    {" "}
                    {lastPriceLinkOverwriteSyncTime?.result === "success"
                      ? "성공"
                      : "실패"}
                  </span>
                </CardDescription>
                {/* <div className="flex flex-wrap gap-2">
                                    <Button
                                        className="w-fit"
                                        variant="outline"
                                        onClick={handleCheckCodeRebuildSelective}
                                    >
                                        check_code 조건부 재빌드
                                    </Button>
                                    <Button className="w-fit" onClick={handleLinkRebuild}>
                                        링크 재연결
                                    </Button>
                                    <Button className="w-fit" variant="secondary" onClick={handlePriceLinkRescanNulls}>
                                        갭 정비
                                    </Button>
                                    <Button className="w-fit" onClick={handleSaveAllPricesToUnion}>
                                        모든 가격 저장
                                    </Button>
                                    <Button className="w-fit" onClick={handleCheckCodeRebuild} disabled>
                                        check_code 재빌더(비활성화)
                                    </Button>
                                    <Button className="w-fit" onClick={handleFullRebuild} disabled>
                                        전체 재빌드(비활성화)
                                    </Button>
                                    <Button className="w-fit" variant="destructive" onClick={handleUnionPublicIdBackfill}>
                                        union public_id 백필
                                    </Button>
                                </div> */}
              </div>
            </CardContent>
          </Card>
        </div>
        <div className="flex flex-col gap-2">
          <Card>
            <CardHeader>
              <CardTitle>최소 가격/환율</CardTitle>
              <Separator />
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              {loadingMinimumPrices ? (
                <>
                  <Skeleton className="h-5 w-24" />
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-10 w-full" />
                  ))}
                </>
              ) : (
                <>
                  <p className="text-sm font-semibold text-muted-foreground">최소 가격</p>
                  {minimumPrices.map((cfg) => (
                    <div key={cfg.configGame}>
                      <Separator className="my-2" key={cfg.configGame} />
                    <form
                      key={cfg.configGame}
                      className="flex items-center gap-2"
                      onSubmit={(e) => handleMinimumPriceChange(
                        gameAbbrFromFullName(cfg.configGame), e
                      )}
                    >
                      <span className="w-64 shrink-0 text-sm font-medium">{cfg.configGame}</span>
                      <Input
                        type="number"
                        placeholder="0000000"
                        defaultValue={cfg.configValue}
                        name="minimumPrice"
                        className="w-28"
                      />
                      <Button className="w-fit shrink-0" type="submit">
                        변경
                      </Button>
                    </form>
                    </div>
                  ))}
                </>
              )}
              <Separator />
              {loadingCurrencyRates ? (
                <>
                  <Skeleton className="h-5 w-16" />
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-10 w-full" />
                  ))}
                </>
              ) : (
                <>
                  <p className="text-sm font-semibold text-muted-foreground">환율</p>
                  {currencyRates.map((cfg) => {
                    const abbr = gameAbbrFromFullName(cfg.configGame)
                    return (
                      <div key={cfg.configGame}>
                      <Separator className="my-2" key={cfg.configGame} />
                      <form
                        key={cfg.configGame}
                        className="flex items-center gap-2"
                        onSubmit={(e) => handleExchangeRateChange(abbr, e)}
                      >
                        <span className="w-64 shrink-0 text-sm font-medium">{cfg.configGame}</span>
                        <Input
                          type="number"
                          step="1"
                          min="0"
                          placeholder="환율(예: 1400.00)"
                          name="exchangeRate"
                          defaultValue={cfg.configValue ?? ""}
                          className="w-28"
                        />
                        <Button
                          className="w-fit shrink-0"
                          type="submit"
                          disabled={submittingGame === abbr}
                        >
                          {submittingGame === abbr ? "변경 중…" : "변경"}
                        </Button>
                      </form>
                      </div>
                    )
                  })}
                </>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <div className="flex flex-row gap-2 items-center justify-between">
                <CardTitle>재고 충전</CardTitle>
                <CardDescription>(설정 시간:매일 14시)</CardDescription>
              </div>
              <Separator />
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              <Button className="w-fit" onClick={handleStockCharge}>
                충전 시작
              </Button>
            </CardContent>
          </Card>
        </div>
        <div className="flex items-center gap-3 rounded-lg border border-dashed border-gray-300 bg-gray-50 px-4 py-3">
        <span className="text-sm text-gray-600">
          ProductSearchMap 참조축 리빌드 (카탈로그/판매 row 누락 복구) ※주의:
          제품 데이터가 초기화되니 개발용도 외에는 누르는 것을 금함
        </span>
        <Button onClick={handleRebuildProductSearchMap} variant="destructive">
          리빌드 실행
        </Button>
      </div>
      </div>
    </div>
  );
}
