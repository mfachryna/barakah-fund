package com.barakah.transaction.grpc;

import com.barakah.transaction.entity.TransactionCategory;
import com.barakah.transaction.mapper.TransactionMapper;
import com.barakah.transaction.proto.v1.*;
import com.barakah.transaction.service.TransactionCategoryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TransactionCategoryGrpcService extends TransactionCategoryServiceGrpc.TransactionCategoryServiceImplBase {

    private final TransactionCategoryService categoryService;
    private final TransactionMapper mapper;

    @Override
    public void createCategory(CreateCategoryRequest request, StreamObserver<CreateCategoryResponse> responseObserver) {
        try {
            log.info("Creating transaction category: {}", request.getName());

            TransactionCategory category = categoryService.createCategory(
                    request.getName(),
                    request.getDescription(), request.getIcon(), request.getColor()
            );

            CreateCategoryResponse response = CreateCategoryResponse.newBuilder()
                    .setCategory(mapper.toProto(category))
                    .setMessage("Category created successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error creating category: {}", request.getName(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCategory(GetCategoryRequest request, StreamObserver<GetCategoryResponse> responseObserver) {
        try {
            log.debug("Getting category: {}", request.getCategoryId());

            TransactionCategory category = categoryService.getCategoryById(request.getCategoryId());

            GetCategoryResponse response = GetCategoryResponse.newBuilder()
                    .setCategory(mapper.toProto(category))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting category: {}", request.getCategoryId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void listCategories(ListCategoriesRequest request, StreamObserver<ListCategoriesResponse> responseObserver) {
        try {
            log.debug("Listing categories");

            Pageable pageable = createPageable(request.getPageRequest());
            boolean includeInactive = request.getIncludeInactive();
            boolean includeSystem = request.getIncludeSystem();

            Page<TransactionCategory> categoryPage = categoryService.listCategories(includeInactive, includeSystem, pageable);

            ListCategoriesResponse response = ListCategoriesResponse.newBuilder()
                    .addAllCategories(mapper.toCategoryProtoList(categoryPage.getContent()))
                    .setPageResponse(createPageResponse(categoryPage))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error listing categories", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateCategory(UpdateCategoryRequest request, StreamObserver<UpdateCategoryResponse> responseObserver) {
        try {
            log.info("Updating category: {}", request.getCategoryId());

            TransactionCategory category = categoryService.updateCategory(
                    request.getCategoryId(), request.getName(), request.getDescription(), request.getIcon(), request.getColor(), request.getIsActive()
            );

            UpdateCategoryResponse response = UpdateCategoryResponse.newBuilder()
                    .setCategory(mapper.toProto(category))
                    .setMessage("Category updated successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error updating category: {}", request.getCategoryId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteCategory(DeleteCategoryRequest request, StreamObserver<DeleteCategoryResponse> responseObserver) {
        try {
            log.info("Deleting category: {}", request.getCategoryId());

            categoryService.deleteCategory(request.getCategoryId());

            DeleteCategoryResponse response = DeleteCategoryResponse.newBuilder()
                    .setMessage("Category deleted successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error deleting category: {}", request.getCategoryId(), e);
            responseObserver.onError(e);
        }
    }

    // Helper methods
    private Pageable createPageable(com.barakah.common.proto.v1.PageRequest pageRequest) {
        if (pageRequest == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        if (!pageRequest.getSort().isEmpty()) {
            Sort.Direction direction =
                    "ASC".equalsIgnoreCase(pageRequest.getDirection()) ?
                            Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, pageRequest.getSort());
        }

        return PageRequest.of(pageRequest.getPage(), Math.min(pageRequest.getSize(), 100),
                sort
        );
    }

    private com.barakah.common.proto.v1.PageResponse createPageResponse(Page<?> page) {
        return com.barakah.common.proto.v1.PageResponse.newBuilder()
                .setPage(page.getNumber())
                .setSize(page.getSize())
                .setTotalElements(page.getTotalElements())
                .setTotalPages(page.getTotalPages())
                .setFirst(page.isFirst())
                .setLast(page.isLast())
                .build();
    }
}