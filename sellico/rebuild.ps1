param($name)
# Thêm "clean package" vào sau các tham số -pl và -am
mvn clean package -pl $name -am -DskipTests

# Các lệnh docker tiếp theo giữ nguyên
docker-compose build $name
docker-compose up -d $name