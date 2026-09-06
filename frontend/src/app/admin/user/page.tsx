"use client"

import { useAdminUser } from "@/app/admin/_hooks/useUserHooks"
import PointLogTableCard from "@/app/admin/user/_components/PointLogTableCard"
import PointLogSearchTableCard from "@/app/admin/user/_components/PointLogSearchTableCard"
import UserListCard from "@/app/admin/user/_components/UserListCard"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

export default function AdminUserPage() {
    const {
        //--------------------------------
        // 회원 목록 관련 상태
        userList,
        total,
        currentPage,
        setCurrentPage,
        userMemoMap,
        applyUserListFromPageBody,
        // 회원 목록 관련 핸들러
        handleUserDataChange,
        handleUserPointChange,
        handleUserMemoChange,
        handleUserMemoInputChange,
        handleUserDelete,
        // 포인트 로그 (최신 10건)
        pointLogList,
        pointLogListWithSystem,
        //--------------------------------
    } = useAdminUser()
    return (
        <div className="space-y-6">
            <h2 className="text-2xl font-bold">회원 관리</h2>
            <Tabs defaultValue="user-list">
                <TabsList className="border-2 border-black" defaultValue="user-list">
                    <TabsTrigger value="user-list">회원 목록</TabsTrigger>
                    <TabsTrigger value="user-point-log">로그</TabsTrigger>
                </TabsList>
                <TabsContent value="user-list">
                    <UserListCard
                        userList={userList}
                        total={total}
                        currentPage={currentPage}
                        userMemoMap={userMemoMap}
                        setCurrentPage={setCurrentPage}
                        applyUserListFromPageBody={applyUserListFromPageBody}
                                        handleUserDataChange={handleUserDataChange}
                        handleUserPointChange={handleUserPointChange}
                        handleUserMemoChange={handleUserMemoChange}
                        handleUserMemoInputChange={handleUserMemoInputChange}
                        handleUserDelete={handleUserDelete}
                    />
                    <PointLogTableCard title="회원 포인트 변경 로그(최신 10건)" logs={pointLogList} />
                </TabsContent>
                <TabsContent value="user-point-log">
                    <PointLogSearchTableCard title="회원 포인트 변경 로그 (SYSTEM 포함)" includeSystem />
                </TabsContent>
            </Tabs>
        </div >
    )
}
