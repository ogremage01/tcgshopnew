import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export function NowLoading() {
    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 12 }).map((_, index) => (
                <Card className="h-48" key={index}>
                    <CardHeader>
                        <CardTitle>
                            <Skeleton className="w-1/2 h-4" />
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <Skeleton className="w-full h-4" />
                        <Skeleton className="w-full h-4" />
                    </CardContent>
                </Card>
            ))}
        </div>
    )
}