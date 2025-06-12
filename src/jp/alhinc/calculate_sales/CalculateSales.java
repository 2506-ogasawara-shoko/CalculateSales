package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String COMMODITY_FILE_NOT_EXIST = "商品定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String COMMODITY_FILE_INVALID_FORMAT = "商品定義ファイルのフォーマットが不正です";
	private static final String NOT_CONSECUTIVE_NUMBERS = "売上ファイル名が連番になっていません";
	private static final String AMOUNT_OVERFLOW = "合計金額が10桁を超えました";
	private static final String NO_BRANCH_CODE = "の支店コードが不正です";
	private static final String NO_COMMODITY_CODE = "の商品コードが不正です";
	private static final String RCD_FILE_INVALID_FORMAT = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		//コマンドライン引数が1つ設定されていなかった場合は、
		//エラーメッセージをコンソールに表示(エラー3-1)
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, FILE_NOT_EXIST, branchNames, branchSales, "^[0-9]{3}$",
				FILE_INVALID_FORMAT)) {
			return;
		}

		// 商品定義ファイル読み込み処理(処理内容1-3)
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, COMMODITY_FILE_NOT_EXIST, commodityNames, commoditySales,
				"^[A-Za-z0-9]{8}$", COMMODITY_FILE_INVALID_FORMAT)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1,2-2)
		File[] files = new File(args[0]).listFiles();

		List<File> rcdFiles = new ArrayList<>();

		for (int i = 0; i < files.length; i++) {
			//対象がファイルであり、「数字8桁.rcd」なのか判定
			if (files[i].isFile() && files[i].getName().matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		//2つのファイル名の数字を比較して
		//差が1ではなかったらエラーメッセージを表示(エラー2-1)
		Collections.sort(rcdFiles);

		for (int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			//比較する2つのファイル名の先頭から数字の8文字を切り出しint型に変換
			if ((latter - former) != 1) {
				System.out.println(NOT_CONSECUTIVE_NUMBERS);
				return;
			}
		}

		//rcdFilesに複数の売上ファイルの情報を格納しているので、その数だけ繰り返す。
		for (int i = 0; i < rcdFiles.size(); i++) {

			BufferedReader br = null;

			try {
				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);
				//listを宣言
				List<String> fileContents = new ArrayList<>();

				String line;
				while ((line = br.readLine()) != null) {
					//1行ずつ詰め込む
					fileContents.add(line);
				}

				//売上金額(3行目)をlistから取得
				String sale = fileContents.get(2);

				//売上ファイルの行数が3行ではなかった場合は、
				//エラーメッセージをコンソールに表示(エラー2-5)
				if (fileContents.size() != 3) {
					System.out.println(file.getName() + RCD_FILE_INVALID_FORMAT);
					return;
				}

				//支店情報を保持しているMapに売上ファイルの支店コードが存在しなかった場合、
				//エラーメッセージをコンソールに表示（エラー2-3）
				if (!branchNames.containsKey(fileContents.get(0))) {
					System.out.println(file.getName() + NO_BRANCH_CODE);
					return;
				}

				//売上ファイルの商品コードが商品定義ファイルに該当しなかった場合、
				//エラーメッセージを表示（エラー2-4）
				if (!commodityNames.containsKey(fileContents.get(1))) {
					System.out.println(file.getName() + NO_COMMODITY_CODE);
					return;
				}

				//売上金額が数字ではなかった場合は、
				//エラーメッセージをコンソールに表示(エラー3-2)
				if (!sale.matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//取得したものをlongに変換
				long fileSale = Long.parseLong(sale);

				//売上金額が入っているMapの支店コード(商品コード)から今の合計金額の取得
				//上記を足す
				Long branchsaleAmount = branchSales.get(fileContents.get(0)) + fileSale;
				Long commoditysaleAmount = commoditySales.get(fileContents.get(1)) + fileSale;

				//売上金額が11桁以上の場合、エラーメッセージをコンソールに表示(エラー2-2)
				if (branchsaleAmount >= 10000000000L || commoditysaleAmount >= 10000000000L) {
					System.out.println(AMOUNT_OVERFLOW);
					return;
				}

				//売上金額が入っているMapに格納
				branchSales.put(fileContents.get(0), branchsaleAmount);
				commoditySales.put(fileContents.get(1), commoditysaleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}

		}

		// 支店別(商品別)集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, String notExist, Map<String, String> names,
			Map<String, Long> sales, String format, String invalidFormat) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//支店定義ファイル(商品定義ファイル)が存在しない場合
			//エラーメッセージを表示（エラー1-1,1-3）
			if (!file.exists()) {
				System.out.println(notExist);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2,1-4)
				String[] items = line.split(",");

				//支店定義ファイル(商品定義ファイル)の仕様が満たされていない場合、
				//エラーメッセージを表示（エラー1-2,1-4）
				if ((items.length != 2) || (!items[0].matches(format))) {
					System.out.println(invalidFormat);
					return false;
				}

				names.put(items[0], items[1]);
				sales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1,3-2)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : names.keySet()) {
				bw.write(key + "," + names.get(key) + "," + sales.get(key));
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
